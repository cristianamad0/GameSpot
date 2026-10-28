package com.usta.gamespot.service;

import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.UsuarioRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carga los usuarios de la base de datos para Spring Security. */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  /**
   * Deja el correo en la forma en que se guarda: sin espacios a los lados y en minúsculas. Así
   * "Cliente@Correo.com" y "cliente@correo.com" son el mismo usuario (defecto D-03).
   */
  public static String normalizarCorreo(String correo) {
    return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Usuario usuario =
        usuarioRepository
            .findByEmail(normalizarCorreo(email))
            .orElseThrow(
                () ->
                    new UsernameNotFoundException("No existe un usuario con el correo: " + email));

    return new User(
        usuario.getEmail(),
        usuario.getPasswordHash(),
        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
  }
}
