package com.usta.gamespot.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CACS-7 (HU-04): ajuste manual de puntos que registra el administrador.
 *
 * @param puntos puntos a sumar (positivo) o restar (negativo); no puede ser cero
 * @param descripcion por qué se hace el ajuste; obligatoria y de máximo 500 caracteres
 */
public record AjustePuntosRequest(
    @NotNull(message = "Los puntos son obligatorios") Integer puntos,
    @NotBlank(message = "La descripción del ajuste es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion) {

  /**
   * Regla de validación: un ajuste de cero puntos no cambia nada y la base no lo acepta.
   *
   * @return true si los puntos son null (lo reporta @NotNull) o distintos de cero
   */
  @AssertTrue(message = "Los puntos del ajuste no pueden ser cero")
  public boolean isPuntosDistintosDeCero() {
    return puntos == null || puntos != 0;
  }
}
