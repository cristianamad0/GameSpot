package com.usta.gamespot.exception;

/**
 * CACS-4 (HU-02): se lanza cuando los datos de una reserva no cumplen las reglas del negocio, por
 * ejemplo un bloque que no empieza en :00 o :30 o que queda fuera del horario de atención.
 */
public class ReservaInvalidaException extends RuntimeException {
  /**
   * Crea la excepción con el motivo que verá el cliente.
   *
   * @param mensaje por qué no se puede reservar
   */
  public ReservaInvalidaException(String mensaje) {
    super(mensaje);
  }
}
