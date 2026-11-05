package com.usta.gamespot;

import static com.usta.gamespot.service.AdminReservasService.ZONA_NEGOCIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.usta.gamespot.demo.DatosDemostracion;
import com.usta.gamespot.dto.AuthResponse;
import com.usta.gamespot.dto.LoginRequest;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.MovimientoPuntosRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.service.AdminReservasService;
import com.usta.gamespot.service.AuthService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de los datos de demostración (CACS-16). Con el perfil demo, el mismo que activa {@code
 * ./mvnw spring-boot:test-run}, los datos se cargan al arrancar; con la configuración de las
 * pruebas no se cargan. Cada caso usa su propio contexto y su propio PostgreSQL de Testcontainers.
 */
class DatosDemostracionTests {

  /**
   * Arranque con el perfil demo. Sin {@code @Transactional}: los datos los guarda el cargador al
   * iniciar el contexto, y las pruebas solo los leen.
   */
  @Nested
  @Import(TestcontainersConfiguration.class)
  @SpringBootTest
  @ActiveProfiles("demo")
  class ConPerfilDemo {

    @Autowired private AuthService authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private MovimientoPuntosRepository movimientoPuntosRepository;
    @Autowired private AdminReservasService adminReservasService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DatosDemostracion datosDemostracion;

    // Criterio 1: se puede iniciar sesión como admin y como cliente sin SQL.

    @Test
    @DisplayName("CP-29a Con el perfil demo se inicia sesión como ADMIN y como CLIENTE")
    void seIniciaSesionConLosUsuariosDeDemostracion() {
      AuthResponse admin =
          authService.iniciarSesion(
              new LoginRequest(DatosDemostracion.EMAIL_ADMIN, DatosDemostracion.CONTRASENA_ADMIN));
      assertEquals(Rol.ADMIN, admin.rol());
      assertFalse(admin.token().isBlank());

      for (String email : List.of(DatosDemostracion.EMAIL_LAURA, DatosDemostracion.EMAIL_ANDRES)) {
        AuthResponse cliente =
            authService.iniciarSesion(
                new LoginRequest(email, DatosDemostracion.CONTRASENA_CLIENTE));
        assertEquals(Rol.CLIENTE, cliente.rol());
      }
    }

    // Criterio 2: al menos una reserva en cada estado, con fechas cercanas al día del arranque.

    @Test
    @DisplayName("CP-29b Hay reservas en los cinco estados, entre ayer y mañana")
    void hayReservasEnCadaEstadoConFechasCercanas() {
      LocalDate hoy = LocalDate.now(ZONA_NEGOCIO);
      for (EstadoReserva estado : EstadoReserva.values()) {
        List<Reserva> reservas = reservaRepository.findByEstadoOrderByInicioAsc(estado);
        assertFalse(reservas.isEmpty(), "Falta una reserva en " + estado);
        for (Reserva reserva : reservas) {
          LocalDate dia = reserva.getInicio().atZone(ZONA_NEGOCIO).toLocalDate();
          assertTrue(
              !dia.isBefore(hoy.minusDays(1)) && !dia.isAfter(hoy.plusDays(1)),
              estado + " el " + dia + " no está entre ayer y mañana");
        }
      }

      // La reserva pendiente se puede pagar durante la demostración.
      Reserva pendiente =
          reservaRepository.findByEstadoOrderByInicioAsc(EstadoReserva.PENDIENTE_PAGO).getFirst();
      assertTrue(pendiente.getExpiraEn().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("CP-29c Los clientes tienen puntos y el admin ve una expirada con pago aprobado")
    void puntosYExpiradaConPago() {
      Usuario laura = usuarioRepository.findByEmail(DatosDemostracion.EMAIL_LAURA).orElseThrow();
      // 2 puntos por las dos horas jugadas (trigger de la base) y 10 de bono.
      assertEquals(12, laura.getPuntosFidelidad());
      assertEquals(
          2, movimientoPuntosRepository.findByUsuarioIdOrderByCreadoEnDesc(laura.getId()).size());

      Usuario andres =
          usuarioRepository.findByEmail(DatosDemostracion.EMAIL_ANDRES).orElseThrow();
      assertEquals(5, andres.getPuntosFidelidad());

      assertEquals(1, adminReservasService.listarExpiradasConPagoAprobado().size());
    }

    @Test
    @DisplayName("CP-29d Volver a ejecutar el cargador no duplica los datos")
    void elCargadorNoDuplica() throws Exception {
      long usuarios = usuarioRepository.count();
      long reservas = reservaRepository.count();

      datosDemostracion.run(null);

      assertEquals(usuarios, usuarioRepository.count());
      assertEquals(reservas, reservaRepository.count());
    }

    // Criterio 5: solo valores de prueba, y las contraseñas se guardan cifradas.

    @Test
    @DisplayName("CP-29e Las contraseñas de demostración se guardan cifradas con BCrypt")
    void lasContrasenasSeGuardanCifradas() {
      Usuario admin = usuarioRepository.findByEmail(DatosDemostracion.EMAIL_ADMIN).orElseThrow();
      assertNotEquals(DatosDemostracion.CONTRASENA_ADMIN, admin.getPasswordHash());
      assertTrue(
          passwordEncoder.matches(DatosDemostracion.CONTRASENA_ADMIN, admin.getPasswordHash()));
    }
  }

  /** Configuración de las pruebas automatizadas, sin el perfil demo. */
  @Nested
  @Import(TestcontainersConfiguration.class)
  @SpringBootTest
  @TestPropertySource(properties = "app.cli.registration.enabled=false")
  @Transactional
  class SinPerfilDemo {

    @Autowired private ApplicationContext contexto;
    @Autowired private UsuarioRepository usuarioRepository;

    // Criterios 3 y 4: sin la propiedad, los datos nunca se cargan.

    @Test
    @DisplayName("CP-29f Sin el perfil demo no existe el cargador ni los usuarios de demostración")
    void sinPerfilDemoNoSeCarganDatos() {
      assertTrue(contexto.getBeansOfType(DatosDemostracion.class).isEmpty());
      assertFalse(usuarioRepository.existsByEmail(DatosDemostracion.EMAIL_ADMIN));
      assertFalse(usuarioRepository.existsByEmail(DatosDemostracion.EMAIL_LAURA));
    }
  }
}
