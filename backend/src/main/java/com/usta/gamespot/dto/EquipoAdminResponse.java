package com.usta.gamespot.dto;

import com.usta.gamespot.model.Equipo;

/**
 * CACS-22 (HU-06): equipo tal como lo ve el administrador en la pestaña "Equipos".
 *
 * @param id id del equipo
 * @param nombre nombre del equipo, por ejemplo "Xbox 360 #1"
 * @param modalidadId id de la modalidad
 * @param modalidad nombre de la modalidad
 * @param tarifaHoraPesos tarifa por hora vigente de la modalidad
 * @param modalidadEstaActiva false si toda la modalidad está desactivada
 * @param estaDisponible false si el equipo está fuera de servicio
 * @param reservasActivasFuturas reservas PENDIENTE_PAGO o CONFIRMADA que aún no empiezan
 */
public record EquipoAdminResponse(
    Long id,
    String nombre,
    Long modalidadId,
    String modalidad,
    int tarifaHoraPesos,
    boolean modalidadEstaActiva,
    boolean estaDisponible,
    long reservasActivasFuturas) {

  /**
   * Convierte la entidad. Debe llamarse dentro de la transacción, porque lee la modalidad.
   *
   * @param equipo equipo con su modalidad
   * @param reservasActivasFuturas reservas activas que aún no empiezan
   * @return el equipo para el panel
   */
  public static EquipoAdminResponse de(Equipo equipo, long reservasActivasFuturas) {
    return new EquipoAdminResponse(
        equipo.getId(),
        equipo.getNombre(),
        equipo.getModalidad().getId(),
        equipo.getModalidad().getNombre(),
        equipo.getModalidad().getTarifaHoraPesos(),
        equipo.getModalidad().isEstaActiva(),
        equipo.isEstaDisponible(),
        reservasActivasFuturas);
  }
}
