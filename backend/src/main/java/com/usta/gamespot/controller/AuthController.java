package com.usta.gamespot.controller;

import com.usta.gamespot.dto.AuthResponse;
import com.usta.gamespot.dto.LoginRequest;
import com.usta.gamespot.dto.RegistroRequest;
import com.usta.gamespot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-2: Formulario de registro / login. Expone los dos endpoints que consume el frontend Angular.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Registra un cliente nuevo y le devuelve su token.
   *
   * @param request datos del registro
   * @return 201 con el token y los datos del usuario creado
   */
  @PostMapping("/registro")
  public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
    AuthResponse respuesta = authService.registrar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
  }

  /**
   * Inicia sesión con correo y contraseña.
   *
   * @param request credenciales del usuario
   * @return 200 con el token y los datos del usuario
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
    AuthResponse respuesta = authService.iniciarSesion(request);
    return ResponseEntity.ok(respuesta);
  }
}
