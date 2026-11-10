package com.usta.gamespot.exception;

/** CACS-22 (HU-06): se lanza cuando el administrador pide una modalidad que no existe. */
public class ModalidadNoEncontradaException extends RuntimeException {
  /**
   * Crea la excepción con el id buscado en el mensaje.
   *
   * @param modalidadId id de la modalidad que no se encontró
   */
  public ModalidadNoEncontradaException(Long modalidadId) {
    super("No existe la modalidad con id " + modalidadId);
  }
}
