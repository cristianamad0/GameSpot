package com.usta.gamespot.dto;

import com.usta.gamespot.model.Modalidad;

/**
 * CACS-22 (HU-06): modalidad con su tarifa, tal como la ve el administrador.
 *
 * @param id id de la modalidad
 * @param nombre nombre de la modalidad, por ejemplo "Xbox Series S"
 * @param tarifaHoraPesos tarifa por hora vigente
 * @param estaActiva false si la modalidad no se ofrece para reservar
 * @param cantidadEquipos cuántos equipos tiene la modalidad
 */
public record ModalidadAdminResponse(
    Long id, String nombre, int tarifaHoraPesos, boolean estaActiva, long cantidadEquipos) {

  /**
   * Convierte la entidad.
   *
   * @param modalidad la modalidad
   * @param cantidadEquipos cuántos equipos tiene
   * @return la modalidad para el panel
   */
  public static ModalidadAdminResponse de(Modalidad modalidad, long cantidadEquipos) {
    return new ModalidadAdminResponse(
        modalidad.getId(),
        modalidad.getNombre(),
        modalidad.getTarifaHoraPesos(),
        modalidad.isEstaActiva(),
        cantidadEquipos);
  }
}
