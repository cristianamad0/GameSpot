package com.usta.gamespot.dto;

import tools.jackson.databind.JsonNode;

/**
 * CACS-21 (HU-03): los datos de una transacción de Wompi que usa el sistema, leídos del objeto
 * {@code transaction} de un evento o de {@code GET /transactions/{id}}.
 *
 * @param id id de la transacción en Wompi
 * @param referencia referencia que el sistema envió al checkout
 * @param estado estado en Wompi: PENDING, APPROVED, DECLINED, VOIDED o ERROR
 * @param montoCentavos monto cobrado, en centavos
 * @param moneda moneda (COP)
 * @param metodoPago medio de pago (CARD, NEQUI, PSE...)
 */
public record TransaccionWompi(
    String id,
    String referencia,
    String estado,
    long montoCentavos,
    String moneda,
    String metodoPago) {

  /**
   * Lee una transacción desde el JSON de Wompi.
   *
   * @param transaccion el objeto {@code transaction} (o {@code data} de la consulta)
   * @return la transacción
   */
  public static TransaccionWompi de(JsonNode transaccion) {
    return new TransaccionWompi(
        transaccion.path("id").asString(),
        transaccion.path("reference").asString(),
        transaccion.path("status").asString(),
        transaccion.path("amount_in_cents").asLong(),
        transaccion.path("currency").asString(),
        transaccion.path("payment_method_type").asString(null));
  }
}
