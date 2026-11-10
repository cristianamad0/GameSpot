package com.usta.gamespot.dto;

/**
 * CACS-21 (HU-03): datos para llevar al cliente al Web Checkout de Wompi. El monto y la referencia
 * van firmados, así que el navegador no los puede cambiar.
 *
 * @param pagoId id del pago creado en PENDIENTE
 * @param referencia referencia única del pago
 * @param montoCentavos monto del adelanto, en centavos
 * @param moneda moneda (COP)
 * @param llavePublica llave pública del comercio
 * @param firmaIntegridad firma de integridad de referencia, monto y moneda
 * @param urlRetorno adonde vuelve el cliente al terminar el pago
 * @param urlCheckout URL completa del checkout, lista para abrir
 */
public record CheckoutWompiResponse(
    Long pagoId,
    String referencia,
    Long montoCentavos,
    String moneda,
    String llavePublica,
    String firmaIntegridad,
    String urlRetorno,
    String urlCheckout) {}
