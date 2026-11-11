package com.usta.gamespot.reservas.service;

import com.usta.gamespot.reservas.dto.LoginRequest;
import com.usta.gamespot.reservas.dto.TokenResponse;
import com.usta.gamespot.reservas.exception.CredencialesInvalidasException;
import com.usta.gamespot.reservas.model.Usuario;
import com.usta.gamespot.reservas.repository.UsuarioRepository;
import com.usta.gamespot.reservas.security.JwtService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CACS-26: inicio de sesión. Compara la contraseña con su hash BCrypt y emite el JWT. */
@Service
@RequiredArgsConstructor
public class AutenticacionService {

  private final UsuarioRepository usuarioRepository;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder cifrador = new BCryptPasswordEncoder();

  /**
   * Inicia sesión.
   *
   * @param request correo y contraseña
   * @return el token y los datos del usuario
   * @throws CredencialesInvalidasException si el correo no existe o la contraseña no coincide
   */
  @Transactional(readOnly = true)
  public TokenResponse iniciarSesion(LoginRequest request) {
    Usuario usuario =
        usuarioRepository
            .findByEmailIgnoreCase(request.email().trim())
            .filter(u -> cifrador.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(CredencialesInvalidasException::new);
    Instant ahora = Instant.now();
    return new TokenResponse(
        jwtService.generar(usuario, ahora),
        "Bearer",
        jwtService.vencimiento(ahora),
        usuario.getNombre(),
        usuario.getEmail(),
        usuario.getRol());
  }
}
