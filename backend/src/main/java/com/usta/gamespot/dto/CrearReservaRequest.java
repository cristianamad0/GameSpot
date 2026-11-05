package com.usta.gamespot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * CACS-4 (HU-02): petición del cliente para reservar un equipo. El precio y el adelanto no vienen
 * del cliente: los calcula el servidor con la tarifa y la configuración del negocio.
 *
 * @param equipoId id del equipo a reservar
 * @param inicio comienzo del primer bloque; debe caer en :00 o :30
 * @param duracionMinutos duración total; debe ser múltiplo de 30
 */
public record CrearReservaRequest(
    @NotNull(message = "es obligatorio") Long equipoId,
    @NotNull(message = "es obligatorio") Instant inicio,
    @NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que 0")
        Integer duracionMinutos) {}
