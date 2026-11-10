package com.usta.gamespot.exception;

/**
 * CACS-21 (HU-03): un evento que dice venir de Wompi no trae un checksum válido. No se cambia
 * ningún pago.
 */
public class FirmaWompiInvalidaException extends RuntimeException {
  /** Crea la excepción con un mensaje genérico, sin revelar qué falló. */
  public FirmaWompiInvalidaException() {
    super("La firma del evento de Wompi no es válida");
  }
}
