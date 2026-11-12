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
 * @param estado PENDIENTE_PAGO, CONFIRMADA, CANCELADA o EXPIRADA
 * @param emailCliente correo de quien reservó
 * @param pagarAntesDe hasta cuándo se puede pagar el adelanto; null si ya no está pendiente
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
    String emailCliente,
    Instant pagarAntesDe) {

  /**
   * Convierte la entidad en el DTO de respuesta con el estado que ve el cliente.
   *
   * @param reserva la reserva
   * @param estado estado vigente (EXPIRADA si venció el plazo de pago)
   * @param pagarAntesDe límite para pagar, o null si ya no está pendiente
   * @return el DTO
   */
  public static ReservaResponse de(Reserva reserva, EstadoReserva estado, Instant pagarAntesDe) {
    return new ReservaResponse(
        reserva.getId(),
        reserva.getEquipoId(),
        reserva.getNombreEquipo(),
        reserva.getInicio(),
        reserva.getFin(),
        reserva.getPrecioTotalPesos(),
        reserva.getAdelantoPesos(),
        estado,
        reserva.getUsuario().getEmail(),
        pagarAntesDe);
  }
}
