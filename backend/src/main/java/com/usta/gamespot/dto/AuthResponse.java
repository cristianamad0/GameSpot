package com.usta.gamespot.dto;

import com.usta.gamespot.model.Rol;

public record AuthResponse(
        String token,
        Long id,
        String nombre,
        String email,
        Rol rol
) {}
