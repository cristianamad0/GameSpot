package com.usta.gamespot.model;

/** Estados de un pago. Equivalen a los estados de transacción de Wompi. */
public enum EstadoPago {
  PENDIENTE, // PENDING
  APROBADO, // APPROVED
  RECHAZADO, // DECLINED
  ANULADO, // VOIDED
  ERROR // ERROR
}
