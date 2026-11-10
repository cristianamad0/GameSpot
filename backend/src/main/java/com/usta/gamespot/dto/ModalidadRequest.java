package com.usta.gamespot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * CACS-22 (HU-06): petición para cambiar la tarifa de una modalidad y activarla o desactivarla.
 *
 * @param tarifaHoraPesos nueva tarifa por hora; entre 1 y {@link #TARIFA_MAXIMA_PESOS}
 * @param estaActiva false para dejar de ofrecer la modalidad
 */
public record ModalidadRequest(
    @NotNull(message = "es obligatoria")
        @Positive(message = "debe ser mayor que 0")
        @Max(value = TARIFA_MAXIMA_PESOS, message = "no puede superar 1.000.000")
        Integer tarifaHoraPesos,
    @NotNull(message = "es obligatorio") Boolean estaActiva) {

  /** Tope de la tarifa por hora, para que un error al digitar no deje un precio desbordado. */
  public static final int TARIFA_MAXIMA_PESOS = 1_000_000;
}
