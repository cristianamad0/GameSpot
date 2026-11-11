package com.usta.gamespot.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales que el navegador envía a la consumidora; esta las reenvía al proveedor de reservas.
 *
 * @param email correo del usuario
 * @param password contraseña
 */
public record LoginRequest(
    @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") String email,
    @NotBlank(message = "es obligatoria") String password) {}
