package com.usta.gamespot.exception;

/** CACS-5 (HU-05): se lanza cuando un usuario intenta pagar una reserva que no es suya. */
public class ReservaAjenaException extends RuntimeException {
  /**
   * Crea la excepción para la reserva indicada.
   *
   * @param reservaId id de la reserva
   */
  public ReservaAjenaException(Long reservaId) {
    super("La reserva " + reservaId + " no pertenece al usuario autenticado");
  }
}
