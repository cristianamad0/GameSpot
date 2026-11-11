package com.usta.gamespot.catalogo.dto;

import com.usta.gamespot.catalogo.model.Modalidad;

/**
 * Modalidad con su tarifa por hora.
 *
 * @param id id de la modalidad
 * @param nombre nombre, por ejemplo "Xbox Series S"
 * @param tarifaHoraPesos tarifa por hora en pesos
 * @param estaActiva false si la modalidad no se ofrece
 */
public record ModalidadResponse(Long id, String nombre, int tarifaHoraPesos, boolean estaActiva) {

  /**
   * Convierte la entidad en el DTO de respuesta.
   *
   * @param modalidad la modalidad
   * @return el DTO
   */
  public static ModalidadResponse de(Modalidad modalidad) {
    return new ModalidadResponse(
        modalidad.getId(),
        modalidad.getNombre(),
        modalidad.getTarifaHoraPesos(),
        modalidad.isEstaActiva());
  }
}
