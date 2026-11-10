package com.usta.gamespot.exception;

import com.usta.gamespot.config.ProveedorPagos;

/**
 * CACS-21 (HU-03): se pidió pagar con un proveedor que no es el activo. Por ejemplo, el pago
 * simulado cuando el sistema cobra con Wompi.
 */
public class ProveedorPagosNoDisponibleException extends RuntimeException {
  /**
   * Crea la excepción con el proveedor activo.
   *
   * @param activo proveedor configurado en {@code app.pagos.proveedor}
   */
  public ProveedorPagosNoDisponibleException(ProveedorPagos activo) {
    super("Este medio de pago no está disponible; el proveedor activo es " + activo);
  }
}
