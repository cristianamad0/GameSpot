package com.usta.gamespot.exception;

/**
 * CACS-24 (HU-03): Wompi no respondió a tiempo, respondió con un error o no se pudo conectar. El
 * pago pudo haberse hecho, así que el cliente puede volver a consultar en unos segundos.
 */
public class WompiNoRespondeException extends RuntimeException {
  /**
   * Crea la excepción con un mensaje para el cliente, sin detalles internos.
   *
   * @param causa error original de la llamada a Wompi
   */
  public WompiNoRespondeException(Throwable causa) {
    super("Wompi no respondió. Si ya pagaste, consulta de nuevo en unos segundos", causa);
  }
}
