package com.usta.gamespot.dto;

import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import java.time.Instant;

/**
 * CACS-4 (HU-02): reserva tal como la ve el cliente que la hizo.
 *
 * @param id id de la reserva
 * @param equipoId id del equipo reservado
 * @param nombreEquipo nombre del equipo reservado
 * @param inicio comienzo del primer bloque
 * @param fin final del último bloque
 * @param precioTotalPesos valor total de la reserva
 * @param adelantoPesos valor del adelanto que debe pagar para confirmarla
 * @param estado estado actual de la reserva
 * @param expiraEn límite para pagar el adelanto; null si ya no está PENDIENTE_PAGO
 */
public record ReservaResponse(
    Long id,
    Long equipoId,
    String nombreEquipo,
    Instant inicio,
    Instant fin,
    Integer precioTotalPesos,
    Integer adelantoPesos,
    EstadoReserva estado,
    Instant expiraEn) {

  /**
   * Convierte la entidad en la respuesta del cliente. Debe llamarse dentro de la transacción,
   * porque lee el equipo de la reserva.
   *
   * @param reserva la reserva a convertir
   * @return la respuesta con los datos del equipo
   */
  public static ReservaResponse de(Reserva reserva) {
    return new ReservaResponse(
        reserva.getId(),
        reserva.getEquipo().getId(),
        reserva.getEquipo().getNombre(),
        reserva.getInicio(),
        reserva.getFin(),
        reserva.getPrecioTotalPesos(),
        reserva.getAdelantoPesos(),
        reserva.getEstado(),
        reserva.getExpiraEn());
  }
}
