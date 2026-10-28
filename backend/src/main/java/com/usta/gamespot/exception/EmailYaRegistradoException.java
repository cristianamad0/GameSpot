package com.usta.gamespot.exception;

/** Se lanza al intentar registrar un correo que ya tiene cuenta. */
public class EmailYaRegistradoException extends RuntimeException {
  /**
   * Crea la excepción con el correo repetido en el mensaje.
   *
   * @param email correo que ya está registrado
   */
  public EmailYaRegistradoException(String email) {
    super("Ya existe una cuenta registrada con el correo: " + email);
  }
}
