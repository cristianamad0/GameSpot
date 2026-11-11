package com.usta.gamespot.portal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Disponibilidad de un equipo en un día: resultado de cruzar el catálogo (equipo y horario) con
 * las reservas (bloques ocupados).
 *
 * @param equipo equipo consultado
 * @param fecha día consultado
 * @param atiende false si el local no abre ese día
 * @param bloques bloques de 30 minutos dentro del horario
 */
public record DisponibilidadResponse(
    EquipoDto equipo, LocalDate fecha, boolean atiende, List<Bloque> bloques) {

  /**
   * Un bloque de 30 minutos.
   *
   * @param inicio comienzo
   * @param fin final
   * @param estaLibre true si se puede reservar
   * @param motivo por qué no está libre: OCUPADO, PASADO o FUERA_DE_SERVICIO; null si está libre
   */
  public record Bloque(Instant inicio, Instant fin, boolean estaLibre, String motivo) {}
}
