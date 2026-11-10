package com.usta.gamespot.dto;

import com.usta.gamespot.config.ProveedorPagos;

/**
 * CACS-21 (HU-03): proveedor de pagos activo, para que el frontend sepa qué flujo mostrar.
 *
 * @param proveedor SIMULADO o WOMPI
 */
public record ProveedorPagosResponse(ProveedorPagos proveedor) {}
