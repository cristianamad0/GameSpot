package com.usta.gamespot.dto;

import com.usta.gamespot.model.MovimientoPuntos;
import com.usta.gamespot.model.TipoMovimientoPuntos;
import java.time.Instant;

/**
 * CACS-7 (HU-07): un movimiento del historial de puntos.
 *
 * @param id id del movimiento
 * @param fecha cuándo se registró
 * @param puntos puntos sumados (positivo) o restados (negativo)
 * @param tipo HORAS_JUGADAS o AJUSTE_ADMIN
 * @param descripcion detalle del movimiento
 * @param reservaId reserva que originó los puntos; null en los ajustes del administrador
 */
public record MovimientoPuntosResponse(
    Long id,
    Instant fecha,
    Integer puntos,
    TipoMovimientoPuntos tipo,
    String descripcion,
    Long reservaId) {

  /**
   * Convierte la entidad en la respuesta de la API.
   *
   * @param movimiento movimiento guardado
   * @return la respuesta para el cliente
   */
  public static MovimientoPuntosResponse de(MovimientoPuntos movimiento) {
    return new MovimientoPuntosResponse(
        movimiento.getId(),
        movimiento.getCreadoEn(),
        movimiento.getPuntos(),
        movimiento.getTipo(),
        movimiento.getDescripcion(),
        movimiento.getReserva() == null ? null : movimiento.getReserva().getId());
  }
}
