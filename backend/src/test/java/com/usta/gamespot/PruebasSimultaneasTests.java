package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import com.usta.gamespot.service.UsuarioDetailsService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Pruebas de concurrencia del objetivo 1, cero dobles reservas (CACS-9). Lanzan peticiones HTTP
 * reales al mismo tiempo contra el servidor y el PostgreSQL de Testcontainers.
 *
 * <p>Sin {@code @Transactional}: cada petición corre en su propio hilo y su propia transacción,
 * así que los datos se guardan de verdad. Por eso los usuarios usan correos únicos, las fechas
 * son de 2099 (no las usa ninguna otra clase) y todo se borra al terminar cada prueba.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.cli.registration.enabled=false")
class PruebasSimultaneasTests {

  /** Lunes 1 de junio de 2099, 15:00 en Bogotá (UTC-5). Atiende de 14:00 a 22:00. */
  private static final Instant LUNES_TRES_PM = Instant.parse("2099-06-01T20:00:00Z");

  /** Lunes 8 de junio de 2099, 14:00 en Bogotá: apertura del día. */
  private static final Instant OTRO_LUNES_APERTURA = Instant.parse("2099-06-08T19:00:00Z");

  /** Bloques de 30 minutos entre las 14:00 y las 22:00. */
  private static final int BLOQUES_DEL_LUNES = 16;

  private static final int PETICIONES_SIMULTANEAS = 10;

  private final HttpClient http =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  @Value("${local.server.port}")
  private int puerto;

  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;

  /**
   * Usuarios creados en la prueba actual; se borran con sus reservas y pagos al terminar. Se
   * crean siempre desde el hilo de la prueba, antes de lanzar las peticiones.
   */
  private final List<Usuario> usuariosCreados = new ArrayList<>();

  @AfterEach
  void borrarDatos() {
    for (Usuario usuario : usuariosCreados) {
      List<Reserva> reservas =
          reservaRepository.findByUsuarioIdOrderByInicioDesc(usuario.getId());
      for (Reserva reserva : reservas) {
        pagoRepository.deleteAll(pagoRepository.findByReservaId(reserva.getId()));
      }
      reservaRepository.deleteAll(reservas);
    }
    usuarioRepository.deleteAll(usuariosCreados);
    usuariosCreados.clear();
  }

  private String tokenDeNuevoCliente() {
    Usuario usuario =
        usuarioRepository.save(
            Usuario.builder()
                .nombre("Cliente simultáneo")
                .email(UUID.randomUUID() + "@gamespot.test")
                .passwordHash("hash-de-prueba")
                .rol(Rol.CLIENTE)
                .build());
    usuariosCreados.add(usuario);
    return "Bearer "
        + jwtService.generarToken(usuarioDetailsService.loadUserByUsername(usuario.getEmail()));
  }

  private Equipo equipo(String nombre) {
    return equipoRepository.findAll().stream()
        .filter(e -> e.getNombre().equals(nombre))
        .findFirst()
        .orElseThrow();
  }

  private HttpResponse<String> post(String ruta, String token, String cuerpo) throws Exception {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .header("Authorization", token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build();
    return http.send(peticion, HttpResponse.BodyHandlers.ofString());
  }

  /** Petición de reserva; devuelve la respuesta completa. */
  private HttpResponse<String> reservar(
      String token, Equipo equipo, Instant inicio, int duracionMinutos) throws Exception {
    return post(
        "/api/reservas",
        token,
        """
        {"equipoId": %d, "inicio": "%s", "duracionMinutos": %d}
        """
            .formatted(equipo.getId(), inicio, duracionMinutos));
  }

  /**
   * Ejecuta las tareas a la vez: cada una espera en su hilo hasta que todas están listas y luego
   * salen juntas. Devuelve los códigos HTTP ordenados de menor a mayor.
   */
  private List<Integer> enSimultaneo(List<Callable<HttpResponse<String>>> tareas)
      throws Exception {
    ExecutorService hilos = Executors.newFixedThreadPool(tareas.size());
    CountDownLatch listas = new CountDownLatch(tareas.size());
    CountDownLatch salida = new CountDownLatch(1);
    try {
      List<Future<HttpResponse<String>>> futuros = new ArrayList<>();
      for (Callable<HttpResponse<String>> tarea : tareas) {
        futuros.add(
            hilos.submit(
                () -> {
                  listas.countDown();
                  salida.await();
                  return tarea.call();
                }));
      }
      listas.await(10, TimeUnit.SECONDS);
      salida.countDown();

      List<Integer> codigos = new ArrayList<>();
      for (Future<HttpResponse<String>> futuro : futuros) {
        codigos.add(futuro.get(60, TimeUnit.SECONDS).statusCode());
      }
      Collections.sort(codigos);
      return codigos;
    } finally {
      hilos.shutdownNow();
    }
  }

  private List<Reserva> activasDelEquipo(Equipo equipo, Instant desde, Instant hasta) {
    return reservaRepository.buscarQueSeCruzan(
        equipo.getId(), desde, hasta, EstadoReserva.QUE_OCUPAN_BLOQUE);
  }

  /** Una respuesta 201 y el resto 409: ninguna 500 ni otro código. */
  private static List<Integer> unGanadorYElRestoConflicto(int peticiones) {
    List<Integer> esperado = new ArrayList<>(Collections.nCopies(peticiones, 409));
    esperado.set(0, 201);
    return esperado;
  }

  // Criterio 1: 10 peticiones simultáneas por el mismo equipo y bloque.

  @RepeatedTest(value = 5, name = "{displayName} ({currentRepetition}/{totalRepetitions})")
  @DisplayName("CP-27a Con 10 reservas simultáneas del mismo bloque gana una; el resto, 409")
  void diezReservasDelMismoBloque() throws Exception {
    Equipo equipo = equipo("Xbox 360 #1");
    List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
    for (int i = 0; i < PETICIONES_SIMULTANEAS; i++) {
      String token = tokenDeNuevoCliente();
      tareas.add(() -> reservar(token, equipo, LUNES_TRES_PM, 60));
    }

    assertEquals(unGanadorYElRestoConflicto(PETICIONES_SIMULTANEAS), enSimultaneo(tareas));
    assertEquals(
        1,
        activasDelEquipo(equipo, LUNES_TRES_PM, LUNES_TRES_PM.plus(1, ChronoUnit.HOURS)).size());
  }

  // Criterio 2: bloques que se cruzan parcialmente.

  @RepeatedTest(value = 5, name = "{displayName} ({currentRepetition}/{totalRepetitions})")
  @DisplayName("CP-27b Con reservas simultáneas que se cruzan en parte solo una queda activa")
  void reservasQueSeCruzanEnParte() throws Exception {
    Equipo equipo = equipo("Xbox Series S #1");
    // 15:00-16:00, 15:30-16:30, 15:30-16:00 y 14:30-16:30: todas comparten 15:30-16:00, así
    // que cada una se cruza con todas las demás.
    Instant[] inicios = {
      LUNES_TRES_PM,
      LUNES_TRES_PM.plus(30, ChronoUnit.MINUTES),
      LUNES_TRES_PM.plus(30, ChronoUnit.MINUTES),
      LUNES_TRES_PM.minus(30, ChronoUnit.MINUTES)
    };
    int[] duraciones = {60, 60, 30, 120};
    List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
    for (int i = 0; i < inicios.length; i++) {
      String token = tokenDeNuevoCliente();
      Instant inicio = inicios[i];
      int duracionMinutos = duraciones[i];
      tareas.add(() -> reservar(token, equipo, inicio, duracionMinutos));
    }

    assertEquals(unGanadorYElRestoConflicto(inicios.length), enSimultaneo(tareas));
    assertEquals(
        1,
        activasDelEquipo(
                equipo,
                LUNES_TRES_PM.minus(30, ChronoUnit.MINUTES),
                LUNES_TRES_PM.plus(90, ChronoUnit.MINUTES))
            .size());
  }

  // Criterio 3: bloques contiguos o equipos distintos se aceptan todos.

  @RepeatedTest(value = 5, name = "{displayName} ({currentRepetition}/{totalRepetitions})")
  @DisplayName("CP-27c Reservas simultáneas contiguas o de equipos distintos se aceptan todas")
  void reservasContiguasODeOtrosEquipos() throws Exception {
    Equipo xbox360 = equipo("Xbox 360 #1");
    List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
    // Mismo equipo, bloques seguidos: 15:00-16:00, 16:00-17:00 y 17:00-17:30.
    String token = tokenDeNuevoCliente();
    tareas.add(() -> reservar(token, xbox360, LUNES_TRES_PM, 60));
    tareas.add(() -> reservar(token, xbox360, LUNES_TRES_PM.plus(60, ChronoUnit.MINUTES), 60));
    tareas.add(() -> reservar(token, xbox360, LUNES_TRES_PM.plus(120, ChronoUnit.MINUTES), 30));
    // Mismo bloque de las 15:00 en cada Series S.
    for (int numero = 1; numero <= 4; numero++) {
      Equipo seriesS = equipo("Xbox Series S #" + numero);
      String otroToken = tokenDeNuevoCliente();
      tareas.add(() -> reservar(otroToken, seriesS, LUNES_TRES_PM, 60));
    }

    assertEquals(Collections.nCopies(tareas.size(), 201), enSimultaneo(tareas));
    assertEquals(
        3,
        activasDelEquipo(xbox360, LUNES_TRES_PM, LUNES_TRES_PM.plus(150, ChronoUnit.MINUTES))
            .size());
  }

  // Criterio 4: pagos simultáneos de la misma reserva.

  @RepeatedTest(value = 5, name = "{displayName} ({currentRepetition}/{totalRepetitions})")
  @DisplayName("CP-27d Con pagos aprobados simultáneos de una reserva queda un solo pago APROBADO")
  void pagosSimultaneosDeLaMismaReserva() throws Exception {
    String token = tokenDeNuevoCliente();
    HttpResponse<String> creada = reservar(token, equipo("Xbox 360 #1"), LUNES_TRES_PM, 60);
    assertEquals(201, creada.statusCode());
    long reservaId = ((Number) JsonPath.read(creada.body(), "$.id")).longValue();

    int pagosSimultaneos = 5;
    List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
    for (int i = 0; i < pagosSimultaneos; i++) {
      tareas.add(
          () ->
              post(
                  "/api/reservas/" + reservaId + "/pago",
                  token,
                  "{\"resultadoSimulado\": \"APROBADO\"}"));
    }

    // El primero confirma la reserva; los demás la encuentran CONFIRMADA y reciben 409.
    assertEquals(unGanadorYElRestoConflicto(pagosSimultaneos), enSimultaneo(tareas));
    List<Pago> aprobados =
        pagoRepository.findByReservaId(reservaId).stream()
            .filter(p -> p.getEstado() == EstadoPago.APROBADO)
            .toList();
    assertEquals(1, aprobados.size());
    assertEquals(
        EstadoReserva.CONFIRMADA, reservaRepository.findById(reservaId).orElseThrow().getEstado());
  }

  // Criterio 5: sin resultados intermitentes.

  @Test
  @DisplayName("CP-27e En los 16 bloques de un día, cada carrera de 10 reservas tiene un ganador")
  void cadaBloqueDelDiaTieneUnSoloGanador() throws Exception {
    Equipo equipo = equipo("Sala VIP Xbox Series X");
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < PETICIONES_SIMULTANEAS; i++) {
      tokens.add(tokenDeNuevoCliente());
    }

    for (int bloque = 0; bloque < BLOQUES_DEL_LUNES; bloque++) {
      Instant inicio = OTRO_LUNES_APERTURA.plus(30L * bloque, ChronoUnit.MINUTES);
      List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
      for (String token : tokens) {
        tareas.add(() -> reservar(token, equipo, inicio, 30));
      }
      assertEquals(
          unGanadorYElRestoConflicto(PETICIONES_SIMULTANEAS),
          enSimultaneo(tareas),
          "Carrera del bloque " + bloque);
    }

    List<Reserva> activas =
        activasDelEquipo(
            equipo, OTRO_LUNES_APERTURA, OTRO_LUNES_APERTURA.plus(8, ChronoUnit.HOURS));
    assertEquals(BLOQUES_DEL_LUNES, activas.size());
    assertTrue(
        activas.stream().allMatch(r -> r.getEstado() == EstadoReserva.PENDIENTE_PAGO),
        "Todas las reservas ganadoras deben quedar PENDIENTE_PAGO");
  }
}
