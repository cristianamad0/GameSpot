package com.usta.gamespot.exception;

/** CACS-4 (HU-02): se lanza cuando se pide un equipo que no existe. */
public class EquipoNoEncontradoException extends RuntimeException {
  /**
   * Crea la excepción con el id buscado en el mensaje.
   *
   * @param equipoId id del equipo que no se encontró
   */
  public EquipoNoEncontradoException(Long equipoId) {
    super("No existe el equipo con id " + equipoId);
  }
}
