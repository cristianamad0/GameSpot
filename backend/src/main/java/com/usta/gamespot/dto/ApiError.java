package com.usta.gamespot.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de las respuestas de error de la API.
 *
 * @param timestamp momento en que ocurrió el error
 * @param status código HTTP
 * @param mensaje descripción del error
 * @param detalles errores puntuales, por ejemplo de validación de campos
 */
public record ApiError(LocalDateTime timestamp, int status, String mensaje, List<String> detalles) {
  /**
   * Crea un error con la hora actual.
   *
   * @param status código HTTP
   * @param mensaje descripción del error
   * @param detalles errores puntuales
   * @return el error listo para enviar
   */
  public static ApiError de(int status, String mensaje, List<String> detalles) {
    return new ApiError(LocalDateTime.now(), status, mensaje, detalles);
  }
}
