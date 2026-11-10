package com.usta.gamespot.config;

/**
 * CACS-21 (HU-03): proveedor con el que se cobra el adelanto. Se elige con la propiedad {@code
 * app.pagos.proveedor}; solo uno está activo a la vez.
 */
public enum ProveedorPagos {
  /** Proveedor simulado de CACS-5: sin dinero real; para pruebas y demostración. */
  SIMULADO,
  /** Wompi (Web Checkout); con llaves de prueba, en su entorno Sandbox. */
  WOMPI
}
