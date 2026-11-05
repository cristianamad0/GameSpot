package com.usta.gamespot.dto;

import com.usta.gamespot.model.Rol;

/**
 * Respuesta de registro e inicio de sesión.
 *
 * @param token JWT para las siguientes peticiones
 * @param id identificador del usuario
 * @param nombre nombre del usuario
 * @param email correo del usuario
 * @param rol rol del usuario
 */
public record AuthResponse(String token, Long id, String nombre, String email, Rol rol) {}
