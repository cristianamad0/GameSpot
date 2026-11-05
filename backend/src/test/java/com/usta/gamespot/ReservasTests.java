package com.usta.gamespot;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import com.usta.gamespot.service.UsuarioDetailsService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del módulo de reservas del cliente (HU-02, CACS-4). Llaman a los endpoints con un token
 * JWT real. Usan fechas fijas de 2099 para que las reservas siempre estén en el futuro. Cada
 * prueba corre en una transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class ReservasTests {

  /** Lunes 2 de marzo de 2099, 15:00 en Bogotá (UTC-5). Atiende de 14:00 a 22:00. */
  private static final Instant LUNES_TRES_PM = Instant.parse("2099-03-02T20:00:00Z");

  /** Lunes 9 de marzo de 2099, 14:00 en Bogotá: apertura del día. */
  private static final Instant OTRO_LUNES_APERTURA = Instant.parse("2099-03-09T19:00:00Z");

  /** Sábado 7 de marzo de 2099, 14:00 en Bogotá. El sábado se atiende de 15:00 a 21:00. */
  private static final Instant SABADO_DOS_PM = Instant.parse("2099-03-07T19:00:00Z");

  /** Xbox 360: $5.000 la hora. */
  private static final String XBOX_360 = "Xbox 360 #1";

  /** Xbox Series S: $6.000 la hora. */
  private static final String SERIES_S = "Xbox Series S #1";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;

  private Usuario cliente;
  private String tokenCliente;
  private Equipo xbox360;

  @BeforeEach
  void prepararDatos() {
    cliente =
        usuarioRepository.saveAndFlush(
            Usuario.builder()
                .nombre("Cliente de prueba")
                .email("cliente@gamespot.test")
                .passwordHash("hash-de-prueba")
                .rol(Rol.CLIENTE)
                .build());
    tokenCliente =
        "Bearer "
            + jwtService.generarToken(
                usuarioDetailsService.loadUserByUsername(cliente.getEmail()));
    xbox360 = equipo(XBOX_360);
  }

  private Equipo equipo(String nombre) {
    return equipoRepository.findAll().stream()
        .filter(e -> e.getNombre().equals(nombre))
        .findFirst()
        .orElseThrow();
  }

  private ResultActions reservar(Long equipoId, Instant inicio, int duracionMinutos)
      throws Exception {
    return mockMvc.perform(
        post("/api/reservas")
            .header("Authorization", tokenCliente)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"equipoId": %d, "inicio": "%s", "duracionMinutos": %d}
                """
                    .formatted(equipoId, inicio, duracionMinutos)));
  }

  private ResultActions consultarDisponibilidad(String fecha) throws Exception {
    return mockMvc.perform(
        get("/api/reservas/disponibilidad")
            .header("Authorization", tokenCliente)
            .param("equipoId", xbox360.getId().toString())
            .param("fecha", fecha));
  }

  /** Momento del lunes 9 de marzo de 2099 a tantos minutos de la apertura (14:00). */
  private static Instant lunesMas(int minutos) {
    return OTRO_LUNES_APERTURA.plus(minutos, ChronoUnit.MINUTES);
  }

  /** Reserva guardada directamente en la base, con los datos que exige cada estado. */
  private Reserva reservaEnBase(EstadoReserva estado, Instant inicio, int duracionMinutos) {
    boolean estaPagada = estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA;
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(xbox360)
            .inicio(inicio)
            .fin(inicio.plus(duracionMinutos, ChronoUnit.MINUTES))
            .precioTotalPesos(5000)
            .adelantoPesos(2500)
            .estado(estado)
            .tieneAnticipoPagado(estaPagada)
            .expiraEn(estado == EstadoReserva.PENDIENTE_PAGO ? LUNES_TRES_PM : null)
            .canceladaEn(estado == EstadoReserva.CANCELADA ? LUNES_TRES_PM : null)
            .build());
  }

  // Criterio 1: un cliente autenticado puede crear una reserva válida.

  @Test
  @DisplayName("CP-26a Una reserva válida nace PENDIENTE_PAGO con precio, adelanto y plazo")
  void reservaValidaQuedaPendienteDePago() throws Exception {
    Instant antes = Instant.now();

    String cuerpo =
        reservar(xbox360.getId(), LUNES_TRES_PM, 60)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
            .andExpect(jsonPath("$.equipoId").value(xbox360.getId()))
            .andExpect(jsonPath("$.precioTotalPesos").value(5000))
            .andExpect(jsonPath("$.adelantoPesos").value(2500))
            .andExpect(jsonPath("$.inicio").value("2099-03-02T20:00:00Z"))
            .andExpect(jsonPath("$.fin").value("2099-03-02T21:00:00Z"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Instant despues = Instant.now();
    Instant expiraEn = Instant.parse(JsonPath.read(cuerpo, "$.expiraEn"));
    // 15 minutos para pagar (minutos_espera_pago del negocio).
    assertFalse(expiraEn.isBefore(antes.plus(15, ChronoUnit.MINUTES)));
    assertFalse(expiraEn.isAfter(despues.plus(15, ChronoUnit.MINUTES)));

    long reservaId = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    Reserva guardada = reservaRepository.findById(reservaId).orElseThrow();
    assertEquals(cliente.getId(), guardada.getUsuario().getId());
    assertEquals(EstadoReserva.PENDIENTE_PAGO, guardada.getEstado());
    assertFalse(guardada.isTieneAnticipoPagado());
  }

  @Test
  @DisplayName("CP-26b Una reserva de varios bloques cobra la tarifa de su modalidad por minuto")
  void reservaDeVariosBloquesCalculaElPrecio() throws Exception {
    // Series S a $6.000 la hora, 90 minutos: $9.000; adelanto del 50 %: $4.500.
    reservar(equipo(SERIES_S).getId(), LUNES_TRES_PM, 90)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precioTotalPesos").value(9000))
        .andExpect(jsonPath("$.adelantoPesos").value(4500))
        .andExpect(jsonPath("$.fin").value("2099-03-02T21:30:00Z"));
  }

  // Criterio 2: bloques alineados a :00 o :30 y duración múltiplo de 30 minutos.

  @Test
  @DisplayName("CP-26c Un bloque que no empieza en :00 o :30 responde 400 con mensaje claro")
  void inicioDesalineadoResponde400() throws Exception {
    reservar(xbox360.getId(), LUNES_TRES_PM.plus(15, ChronoUnit.MINUTES), 60)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("La reserva debe empezar a las :00 o a las :30"));
  }

  @Test
  @DisplayName("CP-26d Una duración que no es múltiplo de 30 minutos responde 400")
  void duracionNoMultiploResponde400() throws Exception {
    reservar(xbox360.getId(), LUNES_TRES_PM, 45)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("La duración debe ser múltiplo de 30 minutos"));
  }

  @Test
  @DisplayName("CP-26e Sin equipo, sin inicio o con duración 0 responde 400 por cada campo")
  void datosIncompletosResponden400() throws Exception {
    mockMvc
        .perform(
            post("/api/reservas")
                .header("Authorization", tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracionMinutos\": 0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Datos invalidos"))
        .andExpect(jsonPath("$.detalles", hasSize(3)));
  }

  // Criterio 3: no se puede reservar fuera del horario de atención del día.

  @Test
  @DisplayName("CP-26f Una reserva que empieza antes de la apertura responde 400")
  void antesDeLaAperturaResponde400() throws Exception {
    // Lunes 13:30: el local abre a las 14:00.
    reservar(xbox360.getId(), OTRO_LUNES_APERTURA.minus(30, ChronoUnit.MINUTES), 60)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "La reserva debe estar dentro del horario de atención de ese día: de 14:00"
                        + " a 22:00"));
  }

  @Test
  @DisplayName("CP-26g Una reserva que termina después del cierre responde 400")
  void despuesDelCierreResponde400() throws Exception {
    // Lunes 21:30 por una hora: terminaría 22:30 y el local cierra a las 22:00.
    reservar(xbox360.getId(), OTRO_LUNES_APERTURA.plus(450, ChronoUnit.MINUTES), 60)
        .andExpect(status().isBadRequest());

    // Lunes 21:30 por media hora sí cabe.
    reservar(xbox360.getId(), OTRO_LUNES_APERTURA.plus(450, ChronoUnit.MINUTES), 30)
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("CP-26h El horario depende del día: el sábado a las 14:00 el local está cerrado")
  void elHorarioDependeDelDia() throws Exception {
    reservar(xbox360.getId(), SABADO_DOS_PM, 60)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "La reserva debe estar dentro del horario de atención de ese día: de 15:00"
                        + " a 21:00"));
  }

  @Test
  @DisplayName("CP-26i Un bloque que ya empezó no se puede reservar (400)")
  void bloqueEnElPasadoResponde400() throws Exception {
    // Sábado 10 de octubre de 2026, 15:00 en Bogotá: dentro del horario, pero ya pasó.
    reservar(xbox360.getId(), Instant.parse("2026-10-10T20:00:00Z"), 60)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("No se puede reservar un bloque que ya empezó"));
  }

  @Test
  @DisplayName("CP-26j Reservar un equipo que no existe responde 404")
  void equipoInexistenteResponde404() throws Exception {
    reservar(999_999L, LUNES_TRES_PM, 60).andExpect(status().isNotFound());
  }

  // Criterio 4: no se puede reservar un bloque que se cruce con otra reserva activa.

  @Test
  @DisplayName("CP-26k Un bloque que se cruza con otra reserva activa responde 409")
  void bloqueQueSeCruzaResponde409() throws Exception {
    reservar(xbox360.getId(), LUNES_TRES_PM, 60).andExpect(status().isCreated());

    // 15:30 a 16:30 se cruza con 15:00 a 16:00.
    reservar(xbox360.getId(), LUNES_TRES_PM.plus(30, ChronoUnit.MINUTES), 60)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("El bloque ya está ocupado"));

    // 16:00 a 17:00 empieza justo cuando termina la anterior: no se cruza.
    reservar(xbox360.getId(), LUNES_TRES_PM.plus(60, ChronoUnit.MINUTES), 60)
        .andExpect(status().isCreated());

    // El mismo horario en otro equipo sí está libre.
    reservar(equipo(SERIES_S).getId(), LUNES_TRES_PM, 60).andExpect(status().isCreated());
  }

  @Test
  @DisplayName("CP-26l Un bloque de una reserva EXPIRADA o CANCELADA se puede volver a reservar")
  void bloqueDeReservaInactivaSePuedeReservar() throws Exception {
    reservaEnBase(EstadoReserva.EXPIRADA, LUNES_TRES_PM, 60);
    reservaEnBase(EstadoReserva.CANCELADA, LUNES_TRES_PM, 60);

    reservar(xbox360.getId(), LUNES_TRES_PM, 60).andExpect(status().isCreated());
  }

  // Criterio 5: la disponibilidad muestra ocupados los bloques de las reservas activas.

  @Test
  @DisplayName("CP-26m Disponibilidad: ocupan las reservas activas; EXPIRADA y CANCELADA no")
  void disponibilidadSegunElEstadoDeLasReservas() throws Exception {
    // Lunes de 14:00 a 22:00: 16 bloques; el bloque n empieza a las 14:00 + 30n minutos.
    reservaEnBase(EstadoReserva.PENDIENTE_PAGO, lunesMas(60), 60);
    reservaEnBase(EstadoReserva.CONFIRMADA, lunesMas(120), 30);
    reservaEnBase(EstadoReserva.COMPLETADA, lunesMas(180), 30);
    reservaEnBase(EstadoReserva.EXPIRADA, lunesMas(240), 60);
    reservaEnBase(EstadoReserva.CANCELADA, lunesMas(300), 30);

    consultarDisponibilidad("2099-03-09")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.equipoId").value(xbox360.getId()))
        .andExpect(jsonPath("$.fecha").value("2099-03-09"))
        .andExpect(jsonPath("$.bloques", hasSize(16)))
        .andExpect(jsonPath("$.bloques[0].inicio").value("2099-03-09T19:00:00Z"))
        .andExpect(jsonPath("$.bloques[0].fin").value("2099-03-09T19:30:00Z"))
        .andExpect(jsonPath("$.bloques[15].fin").value("2099-03-10T03:00:00Z"))
        .andExpect(jsonPath("$.bloques[0].estaLibre").value(true))
        // PENDIENTE_PAGO 15:00 a 16:00
        .andExpect(jsonPath("$.bloques[2].estaLibre").value(false))
        .andExpect(jsonPath("$.bloques[3].estaLibre").value(false))
        // CONFIRMADA 16:00 a 16:30
        .andExpect(jsonPath("$.bloques[4].estaLibre").value(false))
        .andExpect(jsonPath("$.bloques[5].estaLibre").value(true))
        // COMPLETADA 17:00 a 17:30
        .andExpect(jsonPath("$.bloques[6].estaLibre").value(false))
        // EXPIRADA 18:00 a 19:00
        .andExpect(jsonPath("$.bloques[8].estaLibre").value(true))
        .andExpect(jsonPath("$.bloques[9].estaLibre").value(true))
        // CANCELADA 19:00 a 19:30
        .andExpect(jsonPath("$.bloques[10].estaLibre").value(true));
  }

  @Test
  @DisplayName("CP-26n Un equipo fuera de servicio no tiene bloques libres ni se puede reservar")
  void equipoFueraDeServicio() throws Exception {
    xbox360.setEstaDisponible(false);
    equipoRepository.saveAndFlush(xbox360);

    consultarDisponibilidad("2099-03-09")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[0].estaLibre").value(false));
    reservar(xbox360.getId(), LUNES_TRES_PM, 60).andExpect(status().isBadRequest());
  }

  // Criterio 6: un usuario sin token recibe 401.

  @Test
  @DisplayName("CP-26o Sin token, reservar y consultar la disponibilidad responden 401")
  void sinTokenResponde401() throws Exception {
    mockMvc
        .perform(
            post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"equipoId\": %d, \"inicio\": \"%s\", \"duracionMinutos\": 60}"
                        .formatted(xbox360.getId(), LUNES_TRES_PM)))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            get("/api/reservas/disponibilidad")
                .param("equipoId", xbox360.getId().toString())
                .param("fecha", "2099-03-09"))
        .andExpect(status().isUnauthorized());

    assertTrue(reservaRepository.findByUsuarioIdOrderByInicioDesc(cliente.getId()).isEmpty());
  }

  // De punta a punta con CACS-5: la reserva creada por el endpoint se puede pagar.

  @Test
  @DisplayName("CP-26p Una reserva creada con el endpoint se paga y queda CONFIRMADA")
  void reservaCreadaSePagaYQuedaConfirmada() throws Exception {
    String cuerpo =
        reservar(xbox360.getId(), LUNES_TRES_PM, 60)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long reservaId = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();

    mockMvc
        .perform(
            post("/api/reservas/" + reservaId + "/pago")
                .header("Authorization", tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultadoSimulado\": \"APROBADO\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.montoCentavos").value(250_000))
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));
  }
}
