package com.usta.gamespot.exception;

/** CACS-6 (HU-06): se lanza cuando el administrador pide una reserva que no existe. */
public class ReservaNoEncontradaException extends RuntimeException {
  /**
   * Crea la excepción con el id buscado en el mensaje.
   *
   * @param reservaId id de la reserva que no se encontró
   */
  public ReservaNoEncontradaException(Long reservaId) {
    super("No existe la reserva con id " + reservaId);
  }
}
