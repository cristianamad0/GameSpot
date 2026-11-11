package com.usta.gamespot.portal.service;

import com.usta.gamespot.portal.cliente.CatalogoCliente;
import com.usta.gamespot.portal.cliente.ReservasCliente;
import com.usta.gamespot.portal.dto.BloqueOcupadoDto;
import com.usta.gamespot.portal.dto.CrearReservaDto;
import com.usta.gamespot.portal.dto.DisponibilidadResponse;
import com.usta.gamespot.portal.dto.EquipoDto;
import com.usta.gamespot.portal.dto.EstadoServiciosResponse;
import com.usta.gamespot.portal.dto.HorarioDto;
import com.usta.gamespot.portal.dto.LoginRequest;
import com.usta.gamespot.portal.dto.ReservaDto;
import com.usta.gamespot.portal.dto.ReservaPortalRequest;
import com.usta.gamespot.portal.dto.TokenDto;
import com.usta.gamespot.portal.exception.ReglaNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CACS-27: lógica de la consumidora. Coordina los dos proveedores: el catálogo dice qué equipo es,
 * cuánto cuesta y en qué horario se atiende; el proveedor de reservas dice qué está ocupado y
 * guarda la reserva. La consumidora no escribe directamente en los datos de ningún proveedor.
 */
@Service
@RequiredArgsConstructor
public class PortalService {

  /** Zona horaria del negocio (Villavicencio, UTC-5). */
  public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

  private static final int MINUTOS_POR_BLOQUE = 30;

  private final CatalogoCliente catalogoCliente;
  private final ReservasCliente reservasCliente;
  private final Clock reloj;

  /**
   * Equipos del catálogo (solo proveedor 1).
   *
   * @return los equipos
   */
  public List<EquipoDto> equipos() {
    return catalogoCliente.listarEquipos();
  }

  /**
   * Inicia sesión (solo proveedor 2).
   *
   * @param request correo y contraseña
   * @return el token
   */
  public TokenDto login(LoginRequest request) {
    return reservasCliente.login(request);
  }

  /**
   * Disponibilidad de un equipo en un día: operación integrada. Toma el equipo y el horario del
   * catálogo, y los bloques ocupados del proveedor de reservas.
   *
   * @param equipoId id del equipo
   * @param fecha día (hora de Bogotá)
   * @return los bloques de 30 minutos con su estado
   */
  public DisponibilidadResponse disponibilidad(Long equipoId, LocalDate fecha) {
    EquipoDto equipo = catalogoCliente.obtenerEquipo(equipoId);
    Optional<HorarioDto> horario = horarioDelDia(fecha);
    if (horario.isEmpty()) {
      return new DisponibilidadResponse(equipo, fecha, false, List.of());
    }
    List<BloqueOcupadoDto> ocupados = reservasCliente.ocupacion(equipoId, fecha);
    Instant ahora = reloj.instant();
    List<DisponibilidadResponse.Bloque> bloques = new ArrayList<>();
    Instant cierre = enNegocio(fecha, horario.get().horaCierre());
    for (Instant inicio = enNegocio(fecha, horario.get().horaApertura());
        inicio.isBefore(cierre);
        inicio = inicio.plusSeconds(MINUTOS_POR_BLOQUE * 60L)) {
      Instant fin = inicio.plusSeconds(MINUTOS_POR_BLOQUE * 60L);
      String motivo = motivoNoLibre(equipo, ocupados, inicio, fin, ahora);
      bloques.add(new DisponibilidadResponse.Bloque(inicio, fin, motivo == null, motivo));
    }
    return new DisponibilidadResponse(equipo, fecha, true, bloques);
  }

  /**
   * Reserva un equipo: operación integrada. Consulta el equipo, su tarifa y el horario en el
   * catálogo, valida que el equipo se pueda reservar y que el bloque quede dentro del horario, y
   * crea la reserva en el proveedor de reservas con el nombre y la tarifa del catálogo.
   *
   * @param request equipo, fecha, hora y duración
   * @param autorizacion encabezado Authorization del navegador
   * @return la reserva creada
   * @throws ReglaNegocioException 409 si el equipo no se puede reservar; 400 si queda fuera del
   *     horario
   */
  public ReservaDto reservar(ReservaPortalRequest request, String autorizacion) {
    EquipoDto equipo = catalogoCliente.obtenerEquipo(request.equipoId());
    if (!equipo.sePuedeReservar()) {
      throw new ReglaNegocioException(
          409, "El equipo " + equipo.nombre() + " está fuera de servicio y no se puede reservar");
    }
    HorarioDto horario =
        horarioDelDia(request.fecha())
            .orElseThrow(() -> new ReglaNegocioException(400, "El local no atiende ese día"));
    LocalTime fin = request.hora().plusMinutes(request.duracionMinutos());
    boolean pasaMedianoche = !fin.isAfter(request.hora());
    if (request.hora().isBefore(horario.horaApertura())
        || pasaMedianoche
        || fin.isAfter(horario.horaCierre())) {
      throw new ReglaNegocioException(
          400,
          "La reserva debe estar dentro del horario de ese día: de "
              + horario.horaApertura()
              + " a "
              + horario.horaCierre());
    }
    CrearReservaDto reserva =
        new CrearReservaDto(
            equipo.id(),
            equipo.nombre(),
            equipo.tarifaHoraPesos(),
            enNegocio(request.fecha(), request.hora()),
            request.duracionMinutos());
    return reservasCliente.crear(reserva, autorizacion);
  }

  /**
   * Reservas del usuario (solo proveedor 2).
   *
   * @param autorizacion encabezado Authorization del navegador
   * @return sus reservas
   */
  public List<ReservaDto> misReservas(String autorizacion) {
    return reservasCliente.mias(autorizacion);
  }

  /**
   * Paga el adelanto o cancela una reserva (solo proveedor 2).
   *
   * @param id id de la reserva
   * @param accion "pago" o "cancelacion"
   * @param autorizacion encabezado Authorization del navegador
   * @return la reserva actualizada
   */
  public ReservaDto cambiarEstadoReserva(Long id, String accion, String autorizacion) {
    return reservasCliente.cambiarEstado(id, accion, autorizacion);
  }

  /**
   * Pone un equipo fuera de servicio o lo habilita (solo proveedor 1, exige rol ADMIN).
   *
   * @param equipoId id del equipo
   * @param estaDisponible nuevo estado
   * @param autorizacion encabezado Authorization del navegador
   * @return el equipo actualizado
   */
  public EquipoDto cambiarDisponibilidadEquipo(
      Long equipoId, boolean estaDisponible, String autorizacion) {
    EquipoDto equipo = catalogoCliente.obtenerEquipo(equipoId);
    return catalogoCliente.cambiarDisponibilidad(equipo, estaDisponible, autorizacion);
  }

  /**
   * Si cada proveedor responde en este momento.
   *
   * @return el estado de los dos servicios
   */
  public EstadoServiciosResponse estadoServicios() {
    return new EstadoServiciosResponse(
        catalogoCliente.estaDisponible() ? "DISPONIBLE" : "NO_DISPONIBLE",
        reservasCliente.estaDisponible() ? "DISPONIBLE" : "NO_DISPONIBLE");
  }

  private Optional<HorarioDto> horarioDelDia(LocalDate fecha) {
    short dia = (short) fecha.getDayOfWeek().getValue();
    return catalogoCliente.listarHorarios().stream().filter(h -> h.diaSemana() == dia).findFirst();
  }

  private static Instant enNegocio(LocalDate fecha, LocalTime hora) {
    return LocalDateTime.of(fecha, hora).atZone(ZONA_NEGOCIO).toInstant();
  }

  private static String motivoNoLibre(
      EquipoDto equipo, List<BloqueOcupadoDto> ocupados, Instant inicio, Instant fin, Instant ahora) {
    if (!equipo.sePuedeReservar()) {
      return "FUERA_DE_SERVICIO";
    }
    if (!inicio.isAfter(ahora)) {
      return "PASADO";
    }
    boolean ocupado =
        ocupados.stream().anyMatch(o -> o.inicio().isBefore(fin) && o.fin().isAfter(inicio));
    return ocupado ? "OCUPADO" : null;
  }
}
