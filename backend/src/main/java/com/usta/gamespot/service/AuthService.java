package com.usta.gamespot.service;

import com.usta.gamespot.dto.AuthResponse;
import com.usta.gamespot.dto.LoginRequest;
import com.usta.gamespot.dto.RegistroRequest;
import com.usta.gamespot.exception.EmailYaRegistradoException;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Registro e inicio de sesión de usuarios. */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UsuarioDetailsService usuarioDetailsService;

  /**
   * Registra un cliente nuevo con la contraseña cifrada y le genera un token.
   *
   * @param request datos del registro
   * @return el token y los datos del usuario creado
   * @throws EmailYaRegistradoException si el correo ya tiene cuenta
   */
  public AuthResponse registrar(RegistroRequest request) {
    String correo = UsuarioDetailsService.normalizarCorreo(request.email());
    if (usuarioRepository.existsByEmail(correo)) {
      throw new EmailYaRegistradoException(correo);
    }

    Usuario usuario =
        Usuario.builder()
            .nombre(request.nombre())
            .email(correo)
            .passwordHash(passwordEncoder.encode(request.password()))
            .rol(Rol.CLIENTE)
            .build();

    usuarioRepository.save(usuario);

    UserDetails userDetails = usuarioDetailsService.loadUserByUsername(usuario.getEmail());
    String token = jwtService.generarToken(userDetails);

    return new AuthResponse(
        token, usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
  }

  /**
   * Valida las credenciales y genera un token.
   *
   * @param request credenciales del usuario
   * @return el token y los datos del usuario
   */
  public AuthResponse iniciarSesion(LoginRequest request) {
    // Si las credenciales son invalidas, esto lanza BadCredentialsException,
    // manejada por el GlobalExceptionHandler como 401.
    String correo = UsuarioDetailsService.normalizarCorreo(request.email());
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(correo, request.password()));

    Usuario usuario = usuarioRepository.findByEmail(correo).orElseThrow();

    UserDetails userDetails = usuarioDetailsService.loadUserByUsername(usuario.getEmail());
    String token = jwtService.generarToken(userDetails);

    return new AuthResponse(
        token, usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
  }
}
