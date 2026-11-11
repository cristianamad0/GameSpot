package com.usta.gamespot.reservas.exception;

/** El usuario tiene token válido pero no puede tocar ese recurso (por ejemplo, reserva ajena). 403. */
public class AccesoDenegadoException extends RuntimeException {

  /**
   * Crea la excepción.
   *
   * @param mensaje explicación para el usuario
   */
  public AccesoDenegadoException(String mensaje) {
    super(mensaje);
  }
}
