package com.usta.gamespot.reservas.security;

import com.usta.gamespot.reservas.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * CACS-26: emite y valida los JWT. El proveedor de catálogo valida los mismos tokens con el mismo
 * secreto (variable de entorno JWT_SECRETO), sin llamar a este proveedor.
 */
@Service
public class JwtService {

  private final SecretKey llave;
  private final long duracionMs;

  /**
   * Crea el servicio con el secreto y la duración configurados.
   *
   * @param secreto secreto HMAC de al menos 32 caracteres
   * @param duracionMs cuánto dura un token, en milisegundos
   */
  public JwtService(
      @Value("${app.jwt.secret}") String secreto,
      @Value("${app.jwt.expiration-ms}") long duracionMs) {
    this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    this.duracionMs = duracionMs;
  }

  /**
   * Firma un token con el correo como sujeto y el rol como claim.
   *
   * @param usuario usuario autenticado
   * @param ahora momento de emisión
   * @return el token
   */
  public String generar(Usuario usuario, Instant ahora) {
    return Jwts.builder()
        .setSubject(usuario.getEmail())
        .claim("rol", usuario.getRol())
        .claim("nombre", usuario.getNombre())
        .setIssuedAt(Date.from(ahora))
        .setExpiration(Date.from(vencimiento(ahora)))
        .signWith(llave)
        .compact();
  }

  /**
   * Momento en que vence un token emitido ahora.
   *
   * @param ahora momento de emisión
   * @return vencimiento
   */
  public Instant vencimiento(Instant ahora) {
    return ahora.plusMillis(duracionMs);
  }

  /**
   * Valida la firma y la fecha de vencimiento.
   *
   * @param token token recibido
   * @return los datos del token
   * @throws io.jsonwebtoken.JwtException si la firma no coincide o el token venció
   */
  public Claims validar(String token) {
    return Jwts.parserBuilder().setSigningKey(llave).build().parseClaimsJws(token).getBody();
  }
}
