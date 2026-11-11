package com.usta.gamespot.portal.dto;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de los errores de la consumidora; mismo formato que el de los proveedores.
 *
 * @param timestamp momento del error
 * @param status código HTTP
 * @param mensaje descripción en español
 * @param detalles errores puntuales
 */
public record ApiError(Instant timestamp, int status, String mensaje, List<String> detalles) {

  /**
   * Crea el error con la hora actual.
   *
   * @param status código HTTP
   * @param mensaje descripción
   * @param detalles errores puntuales
   * @return el error
   */
  public static ApiError de(int status, String mensaje, List<String> detalles) {
    return new ApiError(Instant.now(), status, mensaje, detalles == null ? List.of() : detalles);
  }
}
