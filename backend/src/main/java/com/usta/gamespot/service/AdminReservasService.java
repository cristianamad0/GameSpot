package com.usta.gamespot.service;

import com.usta.gamespot.dto.ReservaAdminResponse;
import com.usta.gamespot.dto.ReservaExpiradaConPagoResponse;
import com.usta.gamespot.exception.ReservaNoEncontradaException;
import com.usta.gamespot.exception.TransicionReservaInvalidaException;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-6 (HU-06): gestión de las reservas del local desde el panel de administración.
 *
 * <p>Ciclo de vida que aplica: {@code PENDIENTE_PAGO | CONFIRMADA → CANCELADA} y {@code CONFIRMADA
 * → COMPLETADA}. Los puntos de fidelidad los otorga la base de datos (trigger
 * otorgar_puntos_al_completar) cuando la reserva pasa a COMPLETADA.
 */
@Service
@RequiredArgsConstructor
public class AdminReservasService {

  /** Zona del negocio: los días del panel se cuentan en hora de Bogotá. */
  public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

  private static final Set<EstadoReserva> QUE_SE_PUEDEN_CANCELAR =
      EnumSet.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA);

  private final ReservaRepository reservaRepository;
  private final PagoRepository pagoRepository;

  /**
   * Lista las reservas que empiezan en el día indicado (hora de Bogotá), con filtros opcionales.
   *
   * @param fecha día a consultar
   * @param equipoId si no es null, solo las reservas de ese equipo
   * @param estado si no es null, solo las reservas en ese estado
   * @return las reservas ordenadas por hora de inicio; lista vacía si no hay ninguna
   */
  @Transactional(readOnly = true)
  public List<ReservaAdminResponse> listarDelDia(
      LocalDate fecha, Long equipoId, EstadoReserva estado) {
    Instant desde = fecha.atStartOfDay(ZONA_NEGOCIO).toInstant();
    Instant hasta = fecha.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant();

    return reservaRepository.buscarQueInicianEntre(desde, hasta).stream()
        .filter(r -> equipoId == null || equipoId.equals(r.getEquipo().getId()))
        .filter(r -> estado == null || estado == r.getEstado())
        .map(ReservaAdminResponse::de)
        .toList();
  }

  /**
   * Cancela una reserva PENDIENTE_PAGO o CONFIRMADA. Al quedar CANCELADA deja de ocupar su bloque
   * (restricción reservas_sin_cruces), así que otro cliente puede reservarlo.
   *
   * @param reservaId id de la reserva
   * @param motivo por qué se cancela; ya validado como no vacío
   * @return la reserva cancelada
   * @throws ReservaNoEncontradaException si la reserva no existe
   * @throws TransicionReservaInvalidaException si la reserva ya está COMPLETADA, EXPIRADA o
   *     CANCELADA
   */
  @Transactional
  public ReservaAdminResponse cancelar(Long reservaId, String motivo) {
    Reserva reserva = buscar(reservaId);
    if (!QUE_SE_PUEDEN_CANCELAR.contains(reserva.getEstado())) {
      throw new TransicionReservaInvalidaException(reservaId, reserva.getEstado(), "cancelar");
    }

    reserva.setEstado(EstadoReserva.CANCELADA);
    reserva.setCanceladaEn(Instant.now());
    reserva.setMotivoCancelacion(motivo.strip());
    return ReservaAdminResponse.de(reservaRepository.saveAndFlush(reserva));
  }

  /**
   * Marca como COMPLETADA una reserva CONFIRMADA cuando el cliente ya jugó. La base de datos le
   * suma al cliente los puntos de fidelidad de esa reserva una sola vez.
   *
   * @param reservaId id de la reserva
   * @return la reserva completada
   * @throws ReservaNoEncontradaException si la reserva no existe
   * @throws TransicionReservaInvalidaException si la reserva no está CONFIRMADA
   */
  @Transactional
  public ReservaAdminResponse completar(Long reservaId) {
    Reserva reserva = buscar(reservaId);
    if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
      throw new TransicionReservaInvalidaException(reservaId, reserva.getEstado(), "completar");
    }

    reserva.setEstado(EstadoReserva.COMPLETADA);
    return ReservaAdminResponse.de(reservaRepository.saveAndFlush(reserva));
  }

  /**
   * Lista las reservas que expiraron aunque su adelanto quedó aprobado (el pago llegó después del
   * plazo). Son casos que el administrador resuelve a mano.
   *
   * @return los casos, de la reserva más reciente a la más antigua
   */
  @Transactional(readOnly = true)
  public List<ReservaExpiradaConPagoResponse> listarExpiradasConPagoAprobado() {
    return pagoRepository
        .buscarPorEstadoYEstadoDeReserva(EstadoPago.APROBADO, EstadoReserva.EXPIRADA)
        .stream()
        .map(ReservaExpiradaConPagoResponse::de)
        .toList();
  }

  private Reserva buscar(Long reservaId) {
    return reservaRepository
        .findById(reservaId)
        .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
  }
}
