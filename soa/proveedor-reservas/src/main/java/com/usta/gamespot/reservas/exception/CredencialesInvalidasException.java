package com.usta.gamespot.reservas.exception;

/** Correo o contraseña incorrectos al iniciar sesión. 401, con un mensaje que no dice cuál falló. */
public class CredencialesInvalidasException extends RuntimeException {

  /** Crea la excepción con un mensaje genérico. */
  public CredencialesInvalidasException() {
    super("Correo o contraseña incorrectos");
  }
}
