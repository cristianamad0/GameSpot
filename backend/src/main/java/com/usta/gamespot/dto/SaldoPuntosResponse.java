package com.usta.gamespot.dto;

import java.util.List;

/**
 * CACS-7 (HU-04): saldo de puntos de un usuario y su historial de movimientos.
 *
 * @param usuarioId id del usuario
 * @param saldoPuntos puntos disponibles
 * @param movimientos movimientos del más reciente al más antiguo
 */
public record SaldoPuntosResponse(
    Long usuarioId, Integer saldoPuntos, List<MovimientoPuntosResponse> movimientos) {}
