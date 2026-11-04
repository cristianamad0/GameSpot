package com.usta.gamespot.dto;

import com.usta.gamespot.model.Pago;
import java.time.Instant;

/**
 * CACS-6 (HU-06): reserva que expiró aunque su adelanto quedó aprobado. El administrador la
 * resuelve a mano (por ejemplo, devolviendo el dinero o reubicando al cliente).
 *
 * @param reserva datos de la reserva expirada
 * @param referenciaPago referencia del pago enviada a Wompi
 * @param montoPagadoCentavos valor aprobado, en centavos como lo maneja Wompi
 * @param pagoAprobadoEn momento en que se registró el pago
 */
public record ReservaExpiradaConPagoResponse(
    ReservaAdminResponse reserva,
    String referenciaPago,
    Long montoPagadoCentavos,
    Instant pagoAprobadoEn) {

  /**
   * Arma la respuesta a partir del pago aprobado. Debe llamarse dentro de la transacción.
   *
   * @param pago pago aprobado de una reserva expirada
   * @return la respuesta con la reserva y los datos del pago
   */
  public static ReservaExpiradaConPagoResponse de(Pago pago) {
    return new ReservaExpiradaConPagoResponse(
        ReservaAdminResponse.de(pago.getReserva()),
        pago.getReferencia(),
        pago.getMontoCentavos(),
        pago.getActualizadoEn());
  }
}
