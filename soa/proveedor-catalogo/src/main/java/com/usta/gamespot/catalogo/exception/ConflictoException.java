package com.usta.gamespot.catalogo.exception;

/** Se lanza cuando la operación choca con el estado actual, por ejemplo un nombre repetido. 409. */
public class ConflictoException extends RuntimeException {

  /**
   * Crea la excepción.
   *
   * @param mensaje explicación para el usuario
   */
  public ConflictoException(String mensaje) {
    super(mensaje);
  }
}
