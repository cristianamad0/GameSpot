package com.usta.gamespot.catalogo.dto;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de todas las respuestas de error del proveedor.
 *
 * @param timestamp momento del error
 * @param status código HTTP
 * @param mensaje descripción del error en español
 * @param detalles errores puntuales, por ejemplo de cada campo
 */
public record ApiError(Instant timestamp, int status, String mensaje, List<String> detalles) {

  /**
   * Crea el error con la hora actual.
   *
   * @param status código HTTP
   * @param mensaje descripción del error
   * @param detalles errores puntuales
   * @return el error
   */
  public static ApiError de(int status, String mensaje, List<String> detalles) {
    return new ApiError(Instant.now(), status, mensaje, detalles);
  }
}
