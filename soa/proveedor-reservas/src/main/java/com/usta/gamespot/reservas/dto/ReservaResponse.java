package com.usta.gamespot.reservas.dto;

import com.usta.gamespot.reservas.model.EstadoReserva;
import com.usta.gamespot.reservas.model.Reserva;
import java.time.Instant;

/**
 * Reserva tal como la entrega el proveedor.
 *
 * @param id id de la reserva
 * @param equipoId id del equipo en el catálogo
 * @param nombreEquipo nombre del equipo al momento de reservar
 * @param inicio comienzo
 * @param fin final
 * @param precioTotalPesos precio total
 * @param adelantoPesos adelanto (50 % del precio)
 * @param estado PENDIENTE_PAGO, CONFIRMADA o CANCELADA
 * @param emailCliente correo de quien reservó
 */
public record ReservaResponse(
    Long id,
    Long equipoId,
    String nombreEquipo,
    Instant inicio,
    Instant fin,
    int precioTotalPesos,
    int adelantoPesos,
    EstadoReserva estado,
    String emailCliente) {

  /**
   * Convierte la entidad en el DTO de respuesta.
   *
   * @param reserva la reserva
   * @return el DTO
   */
  public static ReservaResponse de(Reserva reserva) {
    return new ReservaResponse(
        reserva.getId(),
        reserva.getEquipoId(),
        reserva.getNombreEquipo(),
        reserva.getInicio(),
        reserva.getFin(),
        reserva.getPrecioTotalPesos(),
        reserva.getAdelantoPesos(),
        reserva.getEstado(),
        reserva.getUsuario().getEmail());
  }
}
