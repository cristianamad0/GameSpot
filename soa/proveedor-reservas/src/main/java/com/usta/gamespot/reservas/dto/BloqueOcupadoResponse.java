package com.usta.gamespot.reservas.dto;

import java.time.Instant;

/**
 * Intervalo ocupado por una reserva activa. No incluye datos del cliente: la ocupación es pública.
 *
 * @param inicio comienzo
 * @param fin final
 */
public record BloqueOcupadoResponse(Instant inicio, Instant fin) {}
