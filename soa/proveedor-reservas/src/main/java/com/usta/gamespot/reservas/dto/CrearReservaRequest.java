package com.usta.gamespot.reservas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Datos para crear una reserva. Los envía la aplicación consumidora después de consultar el equipo
 * y su tarifa en el proveedor de catálogo.
 *
 * @param equipoId id del equipo en el catálogo
 * @param nombreEquipo nombre del equipo, que se guarda con la reserva
 * @param tarifaHoraPesos tarifa por hora vigente del equipo
 * @param inicio comienzo del primer bloque (en :00 o :30)
 * @param duracionMinutos duración, múltiplo de 30, de 30 a 240 minutos
 */
public record CrearReservaRequest(
    @NotNull(message = "es obligatorio") @Positive(message = "debe ser positivo") Long equipoId,
    @NotBlank(message = "es obligatorio") String nombreEquipo,
    @NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que 0")
        Integer tarifaHoraPesos,
    @NotNull(message = "es obligatorio") Instant inicio,
    @NotNull(message = "es obligatoria")
        @Min(value = 30, message = "debe ser de al menos 30 minutos")
        @Max(value = 240, message = "no puede superar 240 minutos")
        Integer duracionMinutos) {}
