package com.usta.gamespot.dto;

import com.usta.gamespot.model.Equipo;
import java.util.List;

/**
 * CACS-19 (HU-02): lo que el cliente necesita para elegir un equipo y saber cuánto pagará antes
 * de reservar.
 *
 * @param porcentajeAdelanto porcentaje del precio que se cobra como adelanto (1 a 100)
 * @param minutosEsperaPago minutos que tiene el cliente para pagar el adelanto
 * @param equipos equipos del local, del más económico al más costoso
 */
public record CatalogoResponse(
    int porcentajeAdelanto, int minutosEsperaPago, List<EquipoCatalogo> equipos) {

  /**
   * Un equipo del catálogo.
   *
   * @param id id del equipo
   * @param nombre nombre del equipo, por ejemplo "Xbox 360 #1"
   * @param modalidad nombre de la modalidad, por ejemplo "Xbox Series S"
   * @param tarifaHoraPesos tarifa por hora de la modalidad
   * @param estaDisponible false si el equipo o su modalidad están fuera de servicio
   */
  public record EquipoCatalogo(
      Long id, String nombre, String modalidad, int tarifaHoraPesos, boolean estaDisponible) {

    /**
     * Convierte la entidad. Debe llamarse dentro de la transacción, porque lee la modalidad.
     *
     * @param equipo equipo con su modalidad
     * @return el equipo para el catálogo
     */
    public static EquipoCatalogo de(Equipo equipo) {
      return new EquipoCatalogo(
          equipo.getId(),
          equipo.getNombre(),
          equipo.getModalidad().getNombre(),
          equipo.getModalidad().getTarifaHoraPesos(),
          equipo.isEstaDisponible() && equipo.getModalidad().isEstaActiva());
    }
  }
}
