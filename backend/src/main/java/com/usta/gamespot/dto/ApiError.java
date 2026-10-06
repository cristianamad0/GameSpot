package com.usta.gamespot.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String mensaje,
        List<String> detalles
) {
    public static ApiError de(int status, String mensaje, List<String> detalles) {
        return new ApiError(LocalDateTime.now(), status, mensaje, detalles);
    }
}
