package com.usta.gamespot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CACS-6 (HU-06): datos para que el administrador cancele una reserva.
 *
 * @param motivo por qué se cancela; obligatorio y de máximo 500 caracteres
 */
public record CancelacionReservaRequest(
    @NotBlank(message = "El motivo de la cancelación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo) {}
