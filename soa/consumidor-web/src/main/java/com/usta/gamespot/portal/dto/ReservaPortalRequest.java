package com.usta.gamespot.portal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que envía el formulario de reserva del navegador. La consumidora completa el resto (nombre y
 * tarifa del equipo) con el proveedor de catálogo.
 *
 * @param equipoId id del equipo
 * @param fecha día de la reserva (hora de Bogotá)
 * @param hora hora de inicio, por ejemplo 15:30
 * @param duracionMinutos duración, de 30 a 240 minutos
 */
public record ReservaPortalRequest(
    @NotNull(message = "es obligatorio") Long equipoId,
    @NotNull(message = "es obligatoria") LocalDate fecha,
    @NotNull(message = "es obligatoria") LocalTime hora,
    @NotNull(message = "es obligatoria")
        @Min(value = 30, message = "debe ser de al menos 30 minutos")
        @Max(value = 240, message = "no puede superar 240 minutos")
        Integer duracionMinutos) {}
