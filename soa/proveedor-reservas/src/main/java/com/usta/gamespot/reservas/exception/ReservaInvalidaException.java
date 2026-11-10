package com.usta.gamespot.reservas.exception;

/** La reserva no cumple las reglas de bloques (en :00 o :30, múltiplo de 30, en el futuro). 400. */
public class ReservaInvalidaException extends RuntimeException {

  /**
   * Crea la excepción.
   *
   * @param mensaje regla que no se cumple
   */
  public ReservaInvalidaException(String mensaje) {
    super(mensaje);
  }
}
