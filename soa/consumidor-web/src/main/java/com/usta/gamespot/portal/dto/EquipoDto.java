package com.usta.gamespot.portal.dto;

/**
 * Equipo tal como lo entrega el proveedor de catálogo (GET /api/equipos).
 *
 * @param id id del equipo
 * @param nombre nombre del equipo
 * @param modalidadId id de su modalidad
 * @param modalidad nombre de la modalidad
 * @param tarifaHoraPesos tarifa por hora vigente
 * @param estaDisponible false si está fuera de servicio
 * @param sePuedeReservar true si está disponible y su modalidad activa
 */
public record EquipoDto(
    Long id,
    String nombre,
    Long modalidadId,
    String modalidad,
    int tarifaHoraPesos,
    boolean estaDisponible,
    boolean sePuedeReservar) {}
