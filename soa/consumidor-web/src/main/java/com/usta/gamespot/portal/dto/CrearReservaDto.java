package com.usta.gamespot.portal.dto;

import java.time.Instant;

/**
 * Cuerpo que la consumidora envía al proveedor de reservas (POST /api/reservas).
 *
 * @param equipoId id del equipo
 * @param nombreEquipo nombre del equipo, tomado del catálogo
 * @param tarifaHoraPesos tarifa vigente, tomada del catálogo
 * @param inicio comienzo del primer bloque
 * @param duracionMinutos duración
 */
public record CrearReservaDto(
    Long equipoId,
    String nombreEquipo,
    int tarifaHoraPesos,
    Instant inicio,
    int duracionMinutos) {}
