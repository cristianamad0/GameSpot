package com.usta.gamespot.catalogo.dto;

import com.usta.gamespot.catalogo.model.Modalidad;

/**
 * Modalidad con su tarifa por hora y por media hora.
 *
 * @param id id de la modalidad
 * @param nombre nombre, por ejemplo "Xbox Series S"
 * @param tarifaHoraPesos tarifa por hora en pesos
 * @param tarifaMediaHoraPesos tarifa de un bloque suelto de 30 minutos en pesos
 * @param estaActiva false si la modalidad no se ofrece
 */
public record ModalidadResponse(
    Long id, String nombre, int tarifaHoraPesos, int tarifaMediaHoraPesos, boolean estaActiva) {

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
        modalidad.getTarifaMediaHoraPesos(),
        modalidad.isEstaActiva());
  }
}
