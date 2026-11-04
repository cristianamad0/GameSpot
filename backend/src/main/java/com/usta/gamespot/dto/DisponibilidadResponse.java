package com.usta.gamespot.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * CACS-4 (HU-02): bloques de 30 minutos de un equipo en un día, dentro del horario de atención.
 *
 * @param equipoId id del equipo consultado
 * @param nombreEquipo nombre del equipo consultado
 * @param fecha día consultado (hora de Bogotá)
 * @param bloques bloques del día en orden; lista vacía si el local no atiende ese día
 */
public record DisponibilidadResponse(
    Long equipoId, String nombreEquipo, LocalDate fecha, List<Bloque> bloques) {

  /**
   * Un bloque de 30 minutos.
   *
   * @param inicio comienzo del bloque
   * @param fin final del bloque
   * @param estaLibre true si se puede reservar; false si lo ocupa una reserva PENDIENTE_PAGO,
   *     CONFIRMADA o COMPLETADA, o si el equipo está fuera de servicio
   */
  public record Bloque(Instant inicio, Instant fin, boolean estaLibre) {}
}
