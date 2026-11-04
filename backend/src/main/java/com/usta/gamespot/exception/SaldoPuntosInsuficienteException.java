package com.usta.gamespot.exception;

/** CACS-7 (HU-04): se lanza cuando un ajuste dejaría el saldo de puntos en negativo. */
public class SaldoPuntosInsuficienteException extends RuntimeException {
  /**
   * Crea la excepción con el saldo actual y el ajuste pedido.
   *
   * @param saldoPuntos saldo actual del usuario
   * @param puntos ajuste que se intentó aplicar
   */
  public SaldoPuntosInsuficienteException(int saldoPuntos, int puntos) {
    super(
        "El ajuste de "
            + puntos
            + " puntos dejaría el saldo en negativo (saldo actual: "
            + saldoPuntos
            + ")");
  }
}
