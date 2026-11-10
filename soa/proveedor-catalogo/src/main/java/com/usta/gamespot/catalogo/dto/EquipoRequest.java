package com.usta.gamespot.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un equipo.
 *
 * @param nombre nombre único del equipo, de 3 a 60 caracteres
 * @param modalidadId id de la modalidad a la que pertenece
 * @param estaDisponible false para dejarlo fuera de servicio
 */
public record EquipoRequest(
    @NotBlank(message = "es obligatorio") @Size(min = 3, max = 60, message = "debe tener entre 3 y 60 caracteres")
        String nombre,
    @NotNull(message = "es obligatoria") Long modalidadId,
    @NotNull(message = "es obligatorio") Boolean estaDisponible) {}
