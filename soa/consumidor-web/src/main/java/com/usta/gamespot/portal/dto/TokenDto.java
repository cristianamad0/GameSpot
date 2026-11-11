package com.usta.gamespot.portal.dto;

import java.time.Instant;

/**
 * Token que devuelve el proveedor de reservas al iniciar sesión.
 *
 * @param token JWT
 * @param tipo "Bearer"
 * @param expiraEn vencimiento
 * @param nombre nombre del usuario
 * @param email correo del usuario
 * @param rol CLIENTE o ADMIN
 */
public record TokenDto(
    String token, String tipo, Instant expiraEn, String nombre, String email, String rol) {}
