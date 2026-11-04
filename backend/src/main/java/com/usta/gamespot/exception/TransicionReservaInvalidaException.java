package com.usta.gamespot.exception;

import com.usta.gamespot.model.EstadoReserva;

/**
 * CACS-6 (HU-06): se lanza cuando se pide un cambio de estado que el ciclo de vida de la reserva
 * no permite, por ejemplo cancelar una reserva ya COMPLETADA.
 */
public class TransicionReservaInvalidaException extends RuntimeException {
  /**
   * Crea la excepción explicando por qué no se puede hacer el cambio.
   *
   * @param reservaId id de la reserva
   * @param estadoActual estado en el que está la reserva
   * @param accion lo que se intentó hacer, en infinitivo (por ejemplo, "cancelar")
   */
  public TransicionReservaInvalidaException(
      Long reservaId, EstadoReserva estadoActual, String accion) {
    super(
        "No se puede " + accion + " la reserva " + reservaId + " porque está en " + estadoActual);
  }
}
