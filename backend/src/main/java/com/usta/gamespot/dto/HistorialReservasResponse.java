package com.usta.gamespot.dto;

import java.util.List;

/**
 * CACS-3 (HU-07): una página del historial de reservas del cliente.
 *
 * @param reservas reservas de la página, de la más reciente a la más antigua
 * @param pagina número de página, empezando en 0
 * @param tamanoPagina cuántas reservas caben en una página
 * @param totalReservas cuántas reservas cumplen el filtro en total
 * @param totalPaginas cuántas páginas hay en total
 */
public record HistorialReservasResponse(
    List<ReservaResponse> reservas,
    int pagina,
    int tamanoPagina,
    long totalReservas,
    int totalPaginas) {}
