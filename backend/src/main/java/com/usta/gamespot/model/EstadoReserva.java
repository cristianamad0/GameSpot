package com.usta.gamespot.model;

import java.util.EnumSet;
import java.util.Set;

/** Estados de una reserva. Se guardan con el mismo nombre en reservas.estado. */
public enum EstadoReserva {
  PENDIENTE_PAGO,
  CONFIRMADA,
  COMPLETADA,
  CANCELADA,
  EXPIRADA;

  /** Estados que ocupan el bloque (los mismos de la restricción reservas_sin_cruces). */
  public static final Set<EstadoReserva> QUE_OCUPAN_BLOQUE =
      EnumSet.of(PENDIENTE_PAGO, CONFIRMADA, COMPLETADA);
}
