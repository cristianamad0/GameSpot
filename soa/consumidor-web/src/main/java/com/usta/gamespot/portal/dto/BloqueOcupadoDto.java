package com.usta.gamespot.portal.dto;

import java.time.Instant;

/**
 * Intervalo ocupado, del proveedor de reservas (GET /api/reservas/ocupacion).
 *
 * @param inicio comienzo
 * @param fin final
 */
public record BloqueOcupadoDto(Instant inicio, Instant fin) {}
