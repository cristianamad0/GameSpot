package com.usta.gamespot.portal.dto;

/**
 * Si cada proveedor responde en este momento. La interfaz lo muestra arriba de la página.
 *
 * @param catalogo DISPONIBLE o NO_DISPONIBLE
 * @param reservas DISPONIBLE o NO_DISPONIBLE
 */
public record EstadoServiciosResponse(String catalogo, String reservas) {}
