package com.usta.gamespot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
