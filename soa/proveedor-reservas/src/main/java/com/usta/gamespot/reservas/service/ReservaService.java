package com.usta.gamespot.reservas.service;

import com.usta.gamespot.reservas.dto.BloqueOcupadoResponse;
import com.usta.gamespot.reservas.dto.CrearReservaRequest;
import com.usta.gamespot.reservas.dto.ReservaResponse;
import com.usta.gamespot.reservas.exception.AccesoDenegadoException;
import com.usta.gamespot.reservas.exception.ConflictoException;
import com.usta.gamespot.reservas.exception.RecursoNoEncontradoException;
import com.usta.gamespot.reservas.exception.ReservaInvalidaException;
import com.usta.gamespot.reservas.model.EstadoReserva;
import com.usta.gamespot.reservas.model.Reserva;
import com.usta.gamespot.reservas.model.Usuario;
import com.usta.gamespot.reservas.repository.ReservaRepository;
import com.usta.gamespot.reservas.repository.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-26: reglas de las reservas. Los bloques empiezan en :00 o :30 y duran múltiplos de 30
 * minutos; un equipo no puede tener dos reservas activas que se crucen; el adelanto es el 50 % del
 * precio. El horario de atención y la disponibilidad del equipo los valida la aplicación
 * consumidora con el proveedor de catálogo antes de llamar aquí.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

  /** Zona horaria del negocio (Villavicencio, UTC-5). */
  public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

  private static final int MINUTOS_POR_BLOQUE = 30;
  private static final int PORCENTAJE_ADELANTO = 50;
  private static final Set<EstadoReserva> ACTIVAS =
      EnumSet.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA);

  private final ReservaRepository reservaRepository;
  private final UsuarioRepository usuarioRepository;

  /**
   * Crea una reserva en PENDIENTE_PAGO para el usuario autenticado.
   *
   * @param request equipo, tarifa, inicio y duración
   * @param email correo del usuario autenticado
   * @return la reserva creada con su precio y adelanto
   * @throws ReservaInvalidaException si el bloque no está alineado o ya empezó
   * @throws ConflictoException si se cruza con otra reserva activa del mismo equipo
   */
  @Transactional
  public ReservaResponse crear(CrearReservaRequest request, String email) {
    Instant inicio = request.inicio();
    int duracionMinutos = request.duracionMinutos();
    if (inicio.getNano() != 0 || inicio.getEpochSecond() % (MINUTOS_POR_BLOQUE * 60) != 0) {
      throw new ReservaInvalidaException("La reserva debe empezar a las :00 o a las :30");
    }
    if (duracionMinutos % MINUTOS_POR_BLOQUE != 0) {
      throw new ReservaInvalidaException("La duración debe ser múltiplo de 30 minutos");
    }
    Instant ahora = Instant.now();
    if (!inicio.isAfter(ahora)) {
      throw new ReservaInvalidaException("No se puede reservar un bloque que ya empezó");
    }
    Instant fin = inicio.plus(duracionMinutos, ChronoUnit.MINUTES);
    if (!reservaRepository
        .buscarQueSeCruzan(request.equipoId(), inicio, fin, ACTIVAS)
        .isEmpty()) {
      throw new ConflictoException("El bloque ya está reservado para ese equipo");
    }
    int precioTotalPesos = Math.ceilDiv(request.tarifaHoraPesos() * duracionMinutos, 60);
    int adelantoPesos = Math.ceilDiv(precioTotalPesos * PORCENTAJE_ADELANTO, 100);
    Reserva reserva =
        Reserva.builder()
            .usuario(buscarUsuario(email))
            .equipoId(request.equipoId())
            .nombreEquipo(request.nombreEquipo().trim())
            .inicio(inicio)
            .fin(fin)
            .precioTotalPesos(precioTotalPesos)
            .adelantoPesos(adelantoPesos)
            .creadaEn(ahora)
            .build();
    return ReservaResponse.de(reservaRepository.save(reserva));
  }

  /**
   * Reservas del usuario autenticado.
   *
   * @param email correo del usuario
   * @return de la más reciente a la más antigua
   */
  @Transactional(readOnly = true)
  public List<ReservaResponse> listarPropias(String email) {
    return reservaRepository.findByUsuarioEmailIgnoreCaseOrderByInicioDesc(email).stream()
        .map(ReservaResponse::de)
        .toList();
  }

  /**
   * Una reserva propia (o cualquiera, si quien consulta es administrador).
   *
   * @param id id de la reserva
   * @param email correo del usuario
   * @param rol rol del usuario
   * @return la reserva
   */
  @Transactional(readOnly = true)
  public ReservaResponse obtener(Long id, String email, String rol) {
    return ReservaResponse.de(buscarAutorizada(id, email, rol));
  }

  /**
   * Bloques ocupados de un equipo en un día (hora de Bogotá). Es información pública: no incluye
   * datos de los clientes.
   *
   * @param equipoId id del equipo
   * @param fecha día a consultar
   * @return intervalos ocupados por reservas activas
   */
  @Transactional(readOnly = true)
  public List<BloqueOcupadoResponse> ocupacion(Long equipoId, LocalDate fecha) {
    Instant desde = fecha.atStartOfDay(ZONA_NEGOCIO).toInstant();
    Instant hasta = fecha.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant();
    return reservaRepository.buscarQueSeCruzan(equipoId, desde, hasta, ACTIVAS).stream()
        .map(r -> new BloqueOcupadoResponse(r.getInicio(), r.getFin()))
        .toList();
  }

  /**
   * Paga el adelanto con el proveedor simulado y confirma la reserva.
   *
   * @param id id de la reserva
   * @param email correo del usuario autenticado
   * @param rol rol del usuario
   * @return la reserva CONFIRMADA
   * @throws ConflictoException si la reserva no está PENDIENTE_PAGO
   */
  @Transactional
  public ReservaResponse pagarAdelanto(Long id, String email, String rol) {
    Reserva reserva = buscarAutorizada(id, email, rol);
    if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
      throw new ConflictoException(
          "Solo se puede pagar una reserva pendiente de pago; esta está " + reserva.getEstado());
    }
    reserva.setEstado(EstadoReserva.CONFIRMADA);
    return ReservaResponse.de(reserva);
  }

  /**
   * Cancela una reserva. Es una cancelación lógica: la reserva queda guardada en estado CANCELADA y
   * su bloque vuelve a estar libre.
   *
   * @param id id de la reserva
   * @param email correo del usuario autenticado
   * @param rol rol del usuario
   * @return la reserva CANCELADA
   * @throws ConflictoException si ya estaba cancelada
   */
  @Transactional
  public ReservaResponse cancelar(Long id, String email, String rol) {
    Reserva reserva = buscarAutorizada(id, email, rol);
    if (reserva.getEstado() == EstadoReserva.CANCELADA) {
      throw new ConflictoException("La reserva " + id + " ya estaba cancelada");
    }
    reserva.setEstado(EstadoReserva.CANCELADA);
    reserva.setCanceladaEn(Instant.now());
    return ReservaResponse.de(reserva);
  }

  private Reserva buscarAutorizada(Long id, String email, String rol) {
    Reserva reserva =
        reservaRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("la reserva", id));
    boolean esDuena = reserva.getUsuario().getEmail().equalsIgnoreCase(email);
    if (!esDuena && !"ADMIN".equals(rol)) {
      throw new AccesoDenegadoException("La reserva " + id + " pertenece a otro usuario");
    }
    return reserva;
  }

  private Usuario buscarUsuario(String email) {
    return usuarioRepository
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new AccesoDenegadoException("El usuario del token ya no existe"));
  }
}
