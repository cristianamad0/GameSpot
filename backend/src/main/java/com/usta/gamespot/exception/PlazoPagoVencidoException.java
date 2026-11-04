package com.usta.gamespot.exception;

/**
 * CACS-5 (HU-03): se lanza cuando se intenta pagar una reserva PENDIENTE_PAGO cuyo plazo ya venció
 * pero que la tarea de expiración todavía no ha pasado a EXPIRADA.
 */
public class PlazoPagoVencidoException extends RuntimeException {
  /**
   * Crea la excepción para la reserva indicada.
   *
   * @param reservaId id de la reserva
   */
  public PlazoPagoVencidoException(Long reservaId) {
    super("El plazo para pagar la reserva " + reservaId + " ya venció");
  }
}
