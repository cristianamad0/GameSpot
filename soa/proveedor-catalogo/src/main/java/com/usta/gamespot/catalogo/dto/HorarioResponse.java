package com.usta.gamespot.catalogo.dto;

import com.usta.gamespot.catalogo.model.HorarioAtencion;
import java.time.LocalTime;

/**
 * Horario de atención de un día.
 *
 * @param diaSemana 1 = lunes ... 7 = domingo
 * @param horaApertura hora de apertura (hora de Bogotá)
 * @param horaCierre hora de cierre (hora de Bogotá)
 */
public record HorarioResponse(short diaSemana, LocalTime horaApertura, LocalTime horaCierre) {

  /**
   * Convierte la entidad en el DTO de respuesta.
   *
   * @param horario horario de un día
   * @return el DTO
   */
  public static HorarioResponse de(HorarioAtencion horario) {
    return new HorarioResponse(
        horario.getDiaSemana(), horario.getHoraApertura(), horario.getHoraCierre());
  }
}
