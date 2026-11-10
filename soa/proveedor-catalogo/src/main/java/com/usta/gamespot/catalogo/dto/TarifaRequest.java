package com.usta.gamespot.catalogo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Nueva tarifa de una modalidad y si se ofrece.
 *
 * @param tarifaHoraPesos tarifa por hora, de 1 a 1.000.000 pesos
 * @param estaActiva false para dejar de ofrecer la modalidad
 */
public record TarifaRequest(
    @NotNull(message = "es obligatoria")
        @Positive(message = "debe ser mayor que 0")
        @Max(value = 1_000_000, message = "no puede superar 1.000.000")
        Integer tarifaHoraPesos,
    @NotNull(message = "es obligatorio") Boolean estaActiva) {}
