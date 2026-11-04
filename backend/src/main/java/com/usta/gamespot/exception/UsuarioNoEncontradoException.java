package com.usta.gamespot.exception;

/** CACS-7 (HU-07): se lanza cuando el administrador pide un usuario que no existe. */
public class UsuarioNoEncontradoException extends RuntimeException {
  /**
   * Crea la excepción con el id buscado en el mensaje.
   *
   * @param usuarioId id del usuario que no se encontró
   */
  public UsuarioNoEncontradoException(Long usuarioId) {
    super("No existe el usuario con id " + usuarioId);
  }
}
