package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.usta.gamespot.dto.AuthResponse;
import com.usta.gamespot.dto.LoginRequest;
import com.usta.gamespot.dto.RegistroRequest;
import com.usta.gamespot.exception.EmailYaRegistradoException;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración del registro e inicio de sesión (HU-01) contra Postgres con el esquema
 * real. Cada prueba corre en una transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class AuthServiceTests {

  private static final String CONTRASENA = "clave-segura-123";

  @Autowired private AuthService authService;
  @Autowired private UsuarioRepository usuarioRepository;

  @Test
  @DisplayName("CP-01 Registro válido crea un cliente con la contraseña cifrada")
  void registroValidoCreaClienteConContrasenaCifrada() {
    AuthResponse respuesta =
        authService.registrar(
            new RegistroRequest("Cliente Prueba", "registro@gamespot.test", CONTRASENA));

    Usuario guardado = usuarioRepository.findByEmail("registro@gamespot.test").orElseThrow();
    assertNotNull(respuesta.id());
    assertEquals(Rol.CLIENTE, respuesta.rol());
    assertFalse(respuesta.token().isBlank());
    assertNotEquals(CONTRASENA, guardado.getPasswordHash());
    assertTrue(guardado.getPasswordHash().startsWith("$2"));
  }

  @Test
  @DisplayName("CP-05 No se permite registrar un correo ya registrado")
  void noPermiteCorreoYaRegistrado() {
    authService.registrar(new RegistroRequest("Primero", "repetido@gamespot.test", CONTRASENA));
    RegistroRequest segundo =
        new RegistroRequest("Segundo", "repetido@gamespot.test", CONTRASENA);

    assertThrows(EmailYaRegistradoException.class, () -> authService.registrar(segundo));
  }

  @Test
  @DisplayName("CP-06 No se permite el mismo correo escrito con otras mayúsculas")
  void noPermiteCorreoRepetidoConOtrasMayusculas() {
    authService.registrar(
        new RegistroRequest("Primero", "Cliente.Mayus@gamespot.test", CONTRASENA));
    RegistroRequest segundo =
        new RegistroRequest("Segundo", "cliente.mayus@gamespot.test", CONTRASENA);

    assertThrows(EmailYaRegistradoException.class, () -> authService.registrar(segundo));
  }

  @ParameterizedTest(name = "correo registrado={0}, contraseña correcta={1} -> autentica={2}")
  @DisplayName("CP-07 Inicio de sesión (tabla de decisión)")
  @CsvSource({"true, true, true", "true, false, false", "false, true, false"})
  void inicioDeSesionSegunTablaDeDecision(
      boolean correoRegistrado, boolean contrasenaCorrecta, boolean debeAutenticar) {
    authService.registrar(new RegistroRequest("Cliente Login", "login@gamespot.test", CONTRASENA));
    String correo = correoRegistrado ? "login@gamespot.test" : "noexiste@gamespot.test";
    String contrasena = contrasenaCorrecta ? CONTRASENA : "otra-clave-999";
    LoginRequest solicitud = new LoginRequest(correo, contrasena);

    if (debeAutenticar) {
      assertFalse(authService.iniciarSesion(solicitud).token().isBlank());
    } else {
      assertThrows(BadCredentialsException.class, () -> authService.iniciarSesion(solicitud));
    }
  }
}
