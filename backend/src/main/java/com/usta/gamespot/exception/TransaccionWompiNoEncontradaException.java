package com.usta.gamespot.exception;

/**
 * CACS-21 (HU-03): Wompi no conoce la transacción consultada, o la transacción no corresponde a un
 * pago de la reserva indicada.
 */
public class TransaccionWompiNoEncontradaException extends RuntimeException {
  /**
   * Crea la excepción para la transacción indicada.
   *
   * @param idTransaccion id de la transacción en Wompi
   */
  public TransaccionWompiNoEncontradaException(String idTransaccion) {
    super("No se encontró la transacción " + idTransaccion + " para esta reserva");
  }
}
