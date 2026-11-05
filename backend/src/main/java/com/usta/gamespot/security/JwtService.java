package com.usta.gamespot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Genera y valida los tokens JWT. */
@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expiration-ms}")
  private long expirationMs;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Genera un token firmado con el correo del usuario como sujeto.
   *
   * @param userDetails usuario autenticado
   * @return el token JWT
   */
  public String generarToken(UserDetails userDetails) {
    Date ahora = new Date();
    Date expiracion = new Date(ahora.getTime() + expirationMs);

    return Jwts.builder()
        .setSubject(userDetails.getUsername())
        .setIssuedAt(ahora)
        .setExpiration(expiracion)
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Lee el correo guardado en el token.
   *
   * @param token token JWT
   * @return el correo del sujeto
   */
  public String extraerEmail(String token) {
    return extraerClaim(token, Claims::getSubject);
  }

  /**
   * Comprueba que el token sea del usuario y que no haya expirado.
   *
   * @param token token JWT
   * @param userDetails usuario contra el que se compara
   * @return {@code true} si el token es válido
   */
  public boolean esTokenValido(String token, UserDetails userDetails) {
    String email = extraerEmail(token);
    return email.equals(userDetails.getUsername()) && !estaExpirado(token);
  }

  private boolean estaExpirado(String token) {
    return extraerClaim(token, Claims::getExpiration).before(new Date());
  }

  private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    return resolver.apply(claims);
  }
}
