package com.usta.gamespot.reservas.model;

/**
 * Ciclo de vida de una reserva: PENDIENTE_PAGO → CONFIRMADA al pagar el adelanto; PENDIENTE_PAGO o
 * CONFIRMADA → CANCELADA. La cancelación es lógica: la reserva no se borra, cambia de estado.
 * CACS-47: PENDIENTE_PAGO → EXPIRADA si el adelanto no se paga dentro de 15 minutos.
 */
public enum EstadoReserva {
  PENDIENTE_PAGO,
  CONFIRMADA,
  CANCELADA,
  /**
   * Reserva sin pagar cuyo plazo de 15 minutos ya pasó. No se guarda en la base: se calcula con la
   * fecha de creación, así que la reserva vence a tiempo aunque nadie la consulte.
   */
  EXPIRADA
}
