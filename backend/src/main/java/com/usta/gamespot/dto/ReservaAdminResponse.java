package com.usta.gamespot.dto;

import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import java.time.Instant;

/**
 * CACS-6 (HU-06): reserva tal como la ve el administrador en el panel.
 *
 * @param id id de la reserva
 * @param usuarioId id del cliente que reservó
 * @param nombreCliente nombre del cliente
 * @param emailCliente correo del cliente
 * @param equipoId id del equipo reservado
 * @param nombreEquipo nombre del equipo reservado
 * @param inicio comienzo del primer bloque
 * @param fin final del último bloque
 * @param precioTotalPesos valor total de la reserva
 * @param adelantoPesos valor del adelanto
 * @param estado estado actual de la reserva
 * @param tieneAnticipoPagado si el adelanto ya se pagó
 * @param canceladaEn momento de la cancelación, o null si no se canceló
 * @param motivoCancelacion motivo de la cancelación, o null si no se canceló
 */
public record ReservaAdminResponse(
    Long id,
    Long usuarioId,
    String nombreCliente,
    String emailCliente,
    Long equipoId,
    String nombreEquipo,
    Instant inicio,
    Instant fin,
    Integer precioTotalPesos,
    Integer adelantoPesos,
    EstadoReserva estado,
    boolean tieneAnticipoPagado,
    Instant canceladaEn,
    String motivoCancelacion) {

  /**
   * Convierte la entidad en la respuesta del panel. Debe llamarse dentro de la transacción,
   * porque lee el usuario y el equipo de la reserva.
   *
   * @param reserva la reserva a convertir
   * @return la respuesta con los datos del cliente y del equipo
   */
  public static ReservaAdminResponse de(Reserva reserva) {
    return new ReservaAdminResponse(
        reserva.getId(),
        reserva.getUsuario().getId(),
        reserva.getUsuario().getNombre(),
        reserva.getUsuario().getEmail(),
        reserva.getEquipo().getId(),
        reserva.getEquipo().getNombre(),
        reserva.getInicio(),
        reserva.getFin(),
        reserva.getPrecioTotalPesos(),
        reserva.getAdelantoPesos(),
        reserva.getEstado(),
        reserva.isTieneAnticipoPagado(),
        reserva.getCanceladaEn(),
        reserva.getMotivoCancelacion());
  }
}
