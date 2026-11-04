package com.usta.gamespot.exception;

/**
 * CACS-4 (HU-02): se lanza cuando la reserva se cruza con otra activa del mismo equipo (restricción
 * reservas_sin_cruces).
 */
public class BloqueOcupadoException extends RuntimeException {
  /** Crea la excepción con el mensaje que verá el cliente. */
  public BloqueOcupadoException() {
    super("El bloque ya está ocupado");
  }
}
