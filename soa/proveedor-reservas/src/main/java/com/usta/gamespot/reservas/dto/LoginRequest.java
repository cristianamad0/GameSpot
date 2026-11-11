package com.usta.gamespot.reservas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales para obtener un token.
 *
 * @param email correo del usuario
 * @param password contraseña
 */
public record LoginRequest(
    @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") String email,
    @NotBlank(message = "es obligatoria") String password) {}
