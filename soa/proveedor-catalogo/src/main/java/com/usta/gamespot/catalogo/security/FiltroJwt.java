package com.usta.gamespot.catalogo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CACS-25: autorización del catálogo. Las consultas (GET) son públicas. Crear, actualizar y
 * eliminar exigen un JWT emitido por el proveedor de reservas, firmado con el mismo secreto, cuyo
 * rol sea ADMIN. Sin token o con un token inválido responde 401; con un rol distinto, 403.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

  private final SecretKey llave;

  /**
   * Crea el filtro con el secreto compartido.
   *
   * @param secreto secreto JWT (variable de entorno JWT_SECRETO), de al menos 32 caracteres
   */
  public FiltroJwt(@Value("${app.jwt.secret}") String secreto) {
    this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String metodo = request.getMethod();
    return !request.getRequestURI().startsWith("/api/")
        || "GET".equals(metodo)
        || "OPTIONS".equals(metodo);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
      throws ServletException, IOException {
    String encabezado = request.getHeader("Authorization");
    if (encabezado == null || !encabezado.startsWith("Bearer ")) {
      rechazar(response, 401, "Falta el token: envía el encabezado Authorization: Bearer <token>");
      return;
    }
    Claims claims;
    try {
      claims =
          Jwts.parserBuilder()
              .setSigningKey(llave)
              .build()
              .parseClaimsJws(encabezado.substring(7))
              .getBody();
    } catch (JwtException | IllegalArgumentException ex) {
      rechazar(response, 401, "El token no es válido o ya venció");
      return;
    }
    if (!"ADMIN".equals(claims.get("rol", String.class))) {
      rechazar(response, 403, "Solo un administrador puede modificar el catálogo");
      return;
    }
    request.setAttribute("usuarioEmail", claims.getSubject());
    cadena.doFilter(request, response);
  }

  private static void rechazar(HttpServletResponse response, int estado, String mensaje)
      throws IOException {
    response.setStatus(estado);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"timestamp\":\""
                + Instant.now()
                + "\",\"status\":"
                + estado
                + ",\"mensaje\":\""
                + mensaje
                + "\",\"detalles\":[]}");
  }
}
