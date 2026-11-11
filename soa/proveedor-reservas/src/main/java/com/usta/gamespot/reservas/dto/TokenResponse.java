package com.usta.gamespot.reservas.dto;

import java.time.Instant;

/**
 * Token emitido al iniciar sesión.
 *
 * @param token JWT firmado; se envía como Authorization: Bearer token
 * @param tipo siempre "Bearer"
 * @param expiraEn momento en que el token deja de servir
 * @param nombre nombre del usuario
 * @param email correo del usuario
 * @param rol CLIENTE o ADMIN
 */
public record TokenResponse(
    String token, String tipo, Instant expiraEn, String nombre, String email, String rol) {}
