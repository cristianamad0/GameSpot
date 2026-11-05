package com.usta.gamespot.service;

import static com.usta.gamespot.service.AdminReservasService.ZONA_NEGOCIO;

import com.usta.gamespot.dto.CrearReservaRequest;
import com.usta.gamespot.dto.DisponibilidadResponse;
import com.usta.gamespot.dto.ReservaResponse;
import com.usta.gamespot.exception.BloqueOcupadoException;
import com.usta.gamespot.exception.EquipoNoEncontradoException;
import com.usta.gamespot.exception.ReservaInvalidaException;
import com.usta.gamespot.model.ConfiguracionNegocio;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.HorarioAtencion;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.ConfiguracionNegocioRepository;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.HorarioAtencionRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-4 (HU-02): reserva de equipos por bloques de 30 minutos y consulta de disponibilidad.
 *
 * <p>Las reglas de bloques y de cruces también las garantiza la base de datos (restricciones
 * reservas_bloques_30_min y reservas_sin_cruces). El servicio las revisa antes para responder con
 * un mensaje claro, y traduce la violación de reservas_sin_cruces cuando dos peticiones
 * simultáneas pasan la revisión al mismo tiempo.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

  private static final int MINUTOS_POR_BLOQUE = 30;
  private static final String RESTRICCION_SIN_CRUCES = "reservas_sin_cruces";

  private final ReservaRepository reservaRepository;
  private final EquipoRepository equipoRepository;
  private final UsuarioRepository usuarioRepository;
  private final HorarioAtencionRepository horarioAtencionRepository;
  private final ConfiguracionNegocioRepository configuracionNegocioRepository;

  /**
   * Crea una reserva en PENDIENTE_PAGO. El precio sale de la tarifa por hora de la modalidad del
   * equipo; el adelanto, del porcentaje de la configuración del negocio. Ambos se redondean hacia
   * arriba al peso. El cliente tiene minutosEsperaPago minutos para pagar el adelanto.
   *
   * @param request equipo, inicio y duración pedidos
   * @param emailUsuario correo del usuario autenticado
   * @return la reserva creada, con su precio, adelanto y fecha límite de pago
   * @throws EquipoNoEncontradoException si el equipo no existe
   * @throws ReservaInvalidaException si el bloque no está alineado a :00 o :30, la duración no es
   *     múltiplo de 30, el bloque ya empezó, queda fuera del horario o el equipo está fuera de
   *     servicio
   * @throws BloqueOcupadoException si se cruza con otra reserva activa del mismo equipo
   */
  @Transactional
  public ReservaResponse crear(CrearReservaRequest request, String emailUsuario) {
    Equipo equipo = buscarEquipo(request.equipoId());
    if (!esReservable(equipo)) {
      throw new ReservaInvalidaException(
          "El equipo " + equipo.getNombre() + " está fuera de servicio");
    }

    Instant inicio = request.inicio();
    int duracionMinutos = request.duracionMinutos();
    validarBloques(inicio, duracionMinutos);
    Instant ahora = Instant.now();
    if (!inicio.isAfter(ahora)) {
      throw new ReservaInvalidaException("No se puede reservar un bloque que ya empezó");
    }
    Instant fin = inicio.plus(duracionMinutos, ChronoUnit.MINUTES);
    validarHorario(inicio, fin);

    if (!reservaRepository
        .buscarQueSeCruzan(equipo.getId(), inicio, fin, EstadoReserva.QUE_OCUPAN_BLOQUE)
        .isEmpty()) {
      throw new BloqueOcupadoException();
    }

    ConfiguracionNegocio configuracion = configuracionNegocioRepository.obtener();
    int precioTotalPesos =
        Math.ceilDiv(equipo.getModalidad().getTarifaHoraPesos() * duracionMinutos, 60);
    int adelantoPesos =
        Math.ceilDiv(precioTotalPesos * configuracion.getPorcentajeAdelanto(), 100);

    Reserva reserva =
        Reserva.builder()
            .usuario(buscarUsuario(emailUsuario))
            .equipo(equipo)
            .inicio(inicio)
            .fin(fin)
            .precioTotalPesos(precioTotalPesos)
            .adelantoPesos(adelantoPesos)
            .expiraEn(ahora.plus(configuracion.getMinutosEsperaPago(), ChronoUnit.MINUTES))
            .build();
    try {
      return ReservaResponse.de(reservaRepository.saveAndFlush(reserva));
    } catch (DataIntegrityViolationException ex) {
      // Otra petición tomó el bloque entre la revisión de cruces y el guardado.
      if (String.valueOf(ex.getMostSpecificCause().getMessage())
          .contains(RESTRICCION_SIN_CRUCES)) {
        throw new BloqueOcupadoException();
      }
      throw ex;
    }
  }

  /**
   * Muestra los bloques de 30 minutos de un equipo en un día, dentro del horario de atención.
   * Ocupan el bloque las reservas PENDIENTE_PAGO, CONFIRMADA o COMPLETADA; las EXPIRADA y
   * CANCELADA lo dejan libre. Si el equipo está fuera de servicio, ningún bloque está libre.
   *
   * @param equipoId id del equipo
   * @param fecha día a consultar (hora de Bogotá)
   * @return los bloques del día en orden
   * @throws EquipoNoEncontradoException si el equipo no existe
   */
  @Transactional(readOnly = true)
  public DisponibilidadResponse consultarDisponibilidad(Long equipoId, LocalDate fecha) {
    Equipo equipo = buscarEquipo(equipoId);
    List<DisponibilidadResponse.Bloque> bloques = new ArrayList<>();

    Optional<HorarioAtencion> horario = buscarHorario(fecha);
    if (horario.isPresent()) {
      Instant apertura =
          fecha.atTime(horario.get().getHoraApertura()).atZone(ZONA_NEGOCIO).toInstant();
      Instant cierre =
          fecha.atTime(horario.get().getHoraCierre()).atZone(ZONA_NEGOCIO).toInstant();
      List<Reserva> ocupan =
          reservaRepository.buscarQueSeCruzan(
              equipoId, apertura, cierre, EstadoReserva.QUE_OCUPAN_BLOQUE);
      boolean esReservable = esReservable(equipo);

      for (Instant inicio = apertura;
          inicio.isBefore(cierre);
          inicio = inicio.plus(MINUTOS_POR_BLOQUE, ChronoUnit.MINUTES)) {
        Instant fin = inicio.plus(MINUTOS_POR_BLOQUE, ChronoUnit.MINUTES);
        bloques.add(
            new DisponibilidadResponse.Bloque(
                inicio, fin, esReservable && !estaOcupado(ocupan, inicio, fin)));
      }
    }
    return new DisponibilidadResponse(equipo.getId(), equipo.getNombre(), fecha, bloques);
  }

  private static boolean estaOcupado(List<Reserva> reservas, Instant inicio, Instant fin) {
    return reservas.stream()
        .anyMatch(r -> r.getInicio().isBefore(fin) && r.getFin().isAfter(inicio));
  }

  private static boolean esReservable(Equipo equipo) {
    return equipo.isEstaDisponible() && equipo.getModalidad().isEstaActiva();
  }

  /** Colombia no tiene horario de verano: alinear en UTC equivale a alinear en Bogotá. */
  private static void validarBloques(Instant inicio, int duracionMinutos) {
    if (inicio.getNano() != 0 || inicio.getEpochSecond() % (MINUTOS_POR_BLOQUE * 60) != 0) {
      throw new ReservaInvalidaException("La reserva debe empezar a las :00 o a las :30");
    }
    if (duracionMinutos % MINUTOS_POR_BLOQUE != 0) {
      throw new ReservaInvalidaException("La duración debe ser múltiplo de 30 minutos");
    }
  }

  private void validarHorario(Instant inicio, Instant fin) {
    LocalDateTime inicioLocal = LocalDateTime.ofInstant(inicio, ZONA_NEGOCIO);
    LocalDateTime finLocal = LocalDateTime.ofInstant(fin, ZONA_NEGOCIO);
    LocalDate dia = inicioLocal.toLocalDate();
    HorarioAtencion horario =
        buscarHorario(dia)
            .orElseThrow(() -> new ReservaInvalidaException("El local no atiende ese día"));

    LocalDateTime apertura = dia.atTime(horario.getHoraApertura());
    LocalDateTime cierre = dia.atTime(horario.getHoraCierre());
    if (inicioLocal.isBefore(apertura) || finLocal.isAfter(cierre)) {
      throw new ReservaInvalidaException(
          "La reserva debe estar dentro del horario de atención de ese día: de "
              + horario.getHoraApertura()
              + " a "
              + horario.getHoraCierre());
    }
  }

  private Optional<HorarioAtencion> buscarHorario(LocalDate dia) {
    return horarioAtencionRepository.findByDiaSemana((short) dia.getDayOfWeek().getValue());
  }

  private Equipo buscarEquipo(Long equipoId) {
    return equipoRepository
        .findById(equipoId)
        .orElseThrow(() -> new EquipoNoEncontradoException(equipoId));
  }

  /** El filtro JWT ya cargó al usuario, así que debe existir. */
  private Usuario buscarUsuario(String email) {
    return usuarioRepository
        .findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("No existe el usuario autenticado " + email));
  }
}
