package com.usta.gamespot.portal.dto;

import java.time.Instant;

/**
 * Reserva tal como la entrega el proveedor de reservas.
 *
 * @param id id de la reserva
 * @param equipoId id del equipo
 * @param nombreEquipo nombre del equipo
 * @param inicio comienzo
 * @param fin final
 * @param precioTotalPesos precio total
 * @param adelantoPesos adelanto
 * @param estado PENDIENTE_PAGO, CONFIRMADA, CANCELADA o EXPIRADA
 * @param emailCliente correo de quien reservó
 * @param pagarAntesDe CACS-47: límite para pagar el adelanto; null si ya no está pendiente
 */
public record ReservaDto(
    Long id,
    Long equipoId,
    String nombreEquipo,
    Instant inicio,
    Instant fin,
    int precioTotalPesos,
    int adelantoPesos,
    String estado,
    String emailCliente,
    Instant pagarAntesDe) {}
