package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import com.usta.gamespot.service.UsuarioDetailsService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Pruebas de los códigos de error con un servidor real (CACS-14). A diferencia de MockMvc, aquí
 * Spring sí reenvía los errores a /error, que es donde aparecía el 401 indebido. Sin
 * {@code @Transactional}: el servidor corre en otro hilo, así que los usuarios se crean con correos
 * únicos.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.cli.registration.enabled=false")
class RespuestasErrorServidorTests {

  private final HttpClient http = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int puerto;

  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;

  private String tokenCliente;
  private String tokenAdmin;

  @BeforeEach
  void prepararUsuarios() {
    tokenCliente = "Bearer " + tokenDe(crearUsuario(Rol.CLIENTE));
    tokenAdmin = "Bearer " + tokenDe(crearUsuario(Rol.ADMIN));
  }

  private Usuario crearUsuario(Rol rol) {
    return usuarioRepository.save(
        Usuario.builder()
            .nombre("Usuario " + rol)
            .email(UUID.randomUUID() + "@gamespot.test")
            .passwordHash("hash-de-prueba")
            .rol(rol)
            .build());
  }

  private String tokenDe(Usuario usuario) {
    return jwtService.generarToken(usuarioDetailsService.loadUserByUsername(usuario.getEmail()));
  }

  private HttpResponse<String> enviar(HttpRequest.Builder peticion) throws Exception {
    return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpRequest.Builder peticion(String ruta, String token) {
    HttpRequest.Builder peticion =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
    return token == null ? peticion : peticion.header("Authorization", token);
  }

  @Test
  @DisplayName("CP-25a Con servidor real, un CLIENTE con token recibe 403 en /api/admin/**")
  void clienteRecibe403EnServidorReal() throws Exception {
    HttpResponse<String> respuesta =
        enviar(peticion("/api/admin/reservas?fecha=2026-11-10", tokenCliente).GET());

    assertEquals(403, respuesta.statusCode());
    // Criterio 4: /error solo describe el error, no devuelve datos de la ruta protegida.
    assertFalse(respuesta.body().contains("emailCliente"));
  }

  @Test
  @DisplayName("CP-25b Con servidor real, una fecha o un enum inválidos responden 400")
  void datosInvalidosResponden400EnServidorReal() throws Exception {
    HttpResponse<String> fechaInvalida =
        enviar(peticion("/api/admin/reservas?fecha=ayer", tokenAdmin).GET());
    HttpResponse<String> enumInvalido =
        enviar(
            peticion("/api/reservas/1/pago", tokenCliente)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"resultadoSimulado\": \"QUIZAS\"}")));

    assertEquals(400, fechaInvalida.statusCode());
    assertEquals(400, enumInvalido.statusCode());
  }

  @Test
  @DisplayName("CP-25c Con servidor real, sin token las rutas protegidas siguen respondiendo 401")
  void sinTokenSigueRespondiendo401() throws Exception {
    assertEquals(401, enviar(peticion("/api/puntos", null).GET()).statusCode());
    assertEquals(
        401, enviar(peticion("/api/admin/reservas?fecha=2026-11-10", null).GET()).statusCode());
  }
}
