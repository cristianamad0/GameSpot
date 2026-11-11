package com.usta.gamespot.reservas.controller;

import com.usta.gamespot.reservas.dto.LoginRequest;
import com.usta.gamespot.reservas.dto.TokenResponse;
import com.usta.gamespot.reservas.service.AutenticacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CACS-26: obtención del JWT. */
@RestController
@RequiredArgsConstructor
public class AutenticacionController {

  private final AutenticacionService autenticacionService;

  /**
   * Inicia sesión y devuelve el token.
   *
   * @param request correo y contraseña
   * @return 200 con el token; 400 datos inválidos; 401 credenciales incorrectas
   */
  @PostMapping("/api/auth/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return autenticacionService.iniciarSesion(request);
  }
}
