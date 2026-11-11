package com.usta.gamespot.portal.dto;

import java.time.Instant;

/**
 * Cuerpo que la consumidora envía al proveedor de reservas (POST /api/reservas).
 *
 * @param equipoId id del equipo
 * @param nombreEquipo nombre del equipo, tomado del catálogo
 * @param tarifaHoraPesos tarifa por hora vigente, tomada del catálogo
 * @param tarifaMediaHoraPesos tarifa de media hora vigente, tomada del catálogo (CACS-30)
 * @param inicio comienzo del primer bloque
 * @param duracionMinutos duración
 */
public record CrearReservaDto(
    Long equipoId,
    String nombreEquipo,
    int tarifaHoraPesos,
    int tarifaMediaHoraPesos,
    Instant inicio,
    int duracionMinutos) {}
