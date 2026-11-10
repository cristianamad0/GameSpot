package com.usta.gamespot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * CACS-22 (HU-06): petición para cambiar la tarifa de una modalidad y activarla o desactivarla.
 *
 * @param tarifaHoraPesos nueva tarifa por hora; debe ser mayor que 0
 * @param estaActiva false para dejar de ofrecer la modalidad
 */
public record ModalidadRequest(
    @NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que 0")
        Integer tarifaHoraPesos,
    @NotNull(message = "es obligatorio") Boolean estaActiva) {}
