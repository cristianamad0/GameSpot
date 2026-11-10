package com.usta.gamespot.dto;

import jakarta.validation.constraints.NotNull;

/**
 * CACS-22 (HU-06): petición para poner un equipo fuera de servicio o volver a habilitarlo.
 *
 * @param estaDisponible true para habilitarlo; false para ponerlo fuera de servicio
 */
public record DisponibilidadEquipoRequest(
    @NotNull(message = "es obligatorio") Boolean estaDisponible) {}
