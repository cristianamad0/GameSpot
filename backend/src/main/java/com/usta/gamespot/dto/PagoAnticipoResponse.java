package com.usta.gamespot.dto;

import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import java.time.Instant;

/**
 * CACS-5 (HU-03): resultado de pagar el adelanto de una reserva.
 *
 * @param pagoId id del pago registrado
 * @param referencia referencia única del pago
 * @param montoCentavos monto cobrado, en centavos de peso
 * @param estadoPago estado del pago (APROBADO o RECHAZADO)
 * @param reservaId id de la reserva
 * @param estadoReserva estado de la reserva después del pago
 * @param tieneAnticipoPagado si la reserva ya tiene el adelanto pagado
 * @param expiraEn límite para pagar; null cuando la reserva ya quedó confirmada
 */
public record PagoAnticipoResponse(
    Long pagoId,
    String referencia,
    Long montoCentavos,
    EstadoPago estadoPago,
    Long reservaId,
    EstadoReserva estadoReserva,
    boolean tieneAnticipoPagado,
    Instant expiraEn) {

  /**
   * Arma la respuesta con el pago y la reserva ya actualizada.
   *
   * @param pago pago registrado
   * @param reserva reserva leída de nuevo después del pago
   * @return la respuesta para el cliente
   */
  public static PagoAnticipoResponse de(Pago pago, Reserva reserva) {
    return new PagoAnticipoResponse(
        pago.getId(),
        pago.getReferencia(),
        pago.getMontoCentavos(),
        pago.getEstado(),
        reserva.getId(),
        reserva.getEstado(),
        reserva.isTieneAnticipoPagado(),
        reserva.getExpiraEn());
  }
}
