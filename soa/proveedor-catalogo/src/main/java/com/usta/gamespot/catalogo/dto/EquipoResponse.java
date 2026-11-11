package com.usta.gamespot.catalogo.dto;

import com.usta.gamespot.catalogo.model.Equipo;

/**
 * Equipo del catálogo tal como lo entrega el proveedor.
 *
 * @param id id del equipo
 * @param nombre nombre del equipo
 * @param modalidadId id de su modalidad
 * @param modalidad nombre de la modalidad
 * @param tarifaHoraPesos tarifa por hora vigente
 * @param estaDisponible false si está fuera de servicio
 * @param sePuedeReservar true si el equipo está disponible y su modalidad activa
 */
public record EquipoResponse(
    Long id,
    String nombre,
    Long modalidadId,
    String modalidad,
    int tarifaHoraPesos,
    boolean estaDisponible,
    boolean sePuedeReservar) {

  /**
   * Convierte la entidad en el DTO de respuesta.
   *
   * @param equipo equipo con su modalidad
   * @return el DTO
   */
  public static EquipoResponse de(Equipo equipo) {
    return new EquipoResponse(
        equipo.getId(),
        equipo.getNombre(),
        equipo.getModalidad().getId(),
        equipo.getModalidad().getNombre(),
        equipo.getModalidad().getTarifaHoraPesos(),
        equipo.isEstaDisponible(),
        equipo.isEstaDisponible() && equipo.getModalidad().isEstaActiva());
  }
}
