package com.usta.gamespot.dto;

/**
 * CACS-5 (HU-03): petición para pagar el adelanto de una reserva. El monto no viene del cliente:
 * lo calcula el servidor a partir del adelanto de la reserva.
 *
 * @param resultadoSimulado resultado que debe dar el proveedor simulado; si es null, APROBADO
 */
public record PagoAnticipoRequest(ResultadoSimulado resultadoSimulado) {}
