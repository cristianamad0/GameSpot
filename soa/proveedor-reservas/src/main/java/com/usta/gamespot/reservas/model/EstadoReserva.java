package com.usta.gamespot.reservas.model;

/**
 * Ciclo de vida de una reserva: PENDIENTE_PAGO → CONFIRMADA al pagar el adelanto; PENDIENTE_PAGO o
 * CONFIRMADA → CANCELADA. La cancelación es lógica: la reserva no se borra, cambia de estado.
 */
public enum EstadoReserva {
  PENDIENTE_PAGO,
  CONFIRMADA,
  CANCELADA
}
