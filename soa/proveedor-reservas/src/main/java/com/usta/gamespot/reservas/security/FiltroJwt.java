package com.usta.gamespot.reservas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CACS-26: autenticación de las reservas. Son públicos el login y la ocupación de un equipo; todo
 * lo demás bajo /api/reservas exige "Authorization: Bearer token". Sin token, o con un token
 * inválido o vencido, responde 401. Si el token es válido, deja el correo y el rol en la petición
 * para que el servicio decida qué puede hacer el usuario.
 */
@Component
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

  /** Atributo de la petición con el correo del usuario autenticado. */
  public static final String EMAIL = "usuarioEmail";

  /** Atributo de la petición con el rol del usuario autenticado. */
  public static final String ROL = "usuarioRol";

  private final JwtService jwtService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String ruta = request.getRequestURI();
    return !ruta.startsWith("/api/reservas")
        || "OPTIONS".equals(request.getMethod())
        || ("GET".equals(request.getMethod()) && ruta.equals("/api/reservas/ocupacion"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
      throws ServletException, IOException {
    String encabezado = request.getHeader("Authorization");
    if (encabezado == null || !encabezado.startsWith("Bearer ")) {
      rechazar(response, "Falta el token: envía el encabezado Authorization: Bearer <token>");
      return;
    }
    Claims claims;
    try {
      claims = jwtService.validar(encabezado.substring(7));
    } catch (JwtException | IllegalArgumentException ex) {
      rechazar(response, "El token no es válido o ya venció");
      return;
    }
    request.setAttribute(EMAIL, claims.getSubject());
    request.setAttribute(ROL, claims.get("rol", String.class));
    cadena.doFilter(request, response);
  }

  private static void rechazar(HttpServletResponse response, String mensaje) throws IOException {
    response.setStatus(401);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"timestamp\":\""
                + Instant.now()
                + "\",\"status\":401,\"mensaje\":\""
                + mensaje
                + "\",\"detalles\":[]}");
  }
}
