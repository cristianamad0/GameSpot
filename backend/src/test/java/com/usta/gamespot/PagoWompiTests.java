package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.config.PropiedadesPagos;
import com.usta.gamespot.config.ProveedorPagos;
import com.usta.gamespot.dto.TransaccionWompi;
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
import com.usta.gamespot.service.ClienteWompi;
import com.usta.gamespot.service.UsuarioDetailsService;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del pago del adelanto con Wompi (HU-03, CACS-21). Ninguna llama a Wompi: los eventos se
 * arman y firman aquí, y la consulta de transacciones usa un Wompi simulado. Las llaves de estas
 * pruebas son valores inventados que solo existen aquí. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "app.cli.registration.enabled=false",
      "app.pagos.proveedor=wompi",
      "app.pagos.wompi.llave-publica=" + PagoWompiTests.LLAVE_PUBLICA,
      "app.pagos.wompi.secreto-eventos=" + PagoWompiTests.SECRETO_EVENTOS,
      "app.pagos.wompi.secreto-integridad=" + PagoWompiTests.SECRETO_INTEGRIDAD
    })
@Transactional
class PagoWompiTests {

  static final String LLAVE_PUBLICA = "llave-publica-solo-pruebas";
  static final String SECRETO_EVENTOS = "secreto-eventos-solo-pruebas";
  static final String SECRETO_INTEGRIDAD = "secreto-integridad-solo-pruebas";

  /** 10 de octubre de 2026, 15:00 en Bogotá (UTC-5). */
  private static final Instant LAS_TRES_PM = Instant.parse("2026-10-10T20:00:00Z");

  private static final Instant PLAZO_VIGENTE = Instant.parse("2099-12-31T05:00:00Z");
  private static final Instant PLAZO_VENCIDO = Instant.parse("2026-10-10T19:00:00Z");

  /** Adelanto de $2.500 = 250.000 centavos. */
  private static final int ADELANTO_PESOS = 2500;

  private static final long ADELANTO_CENTAVOS = ADELANTO_PESOS * 100L;

  /** Momento fijo de los eventos (segundos), como el {@code timestamp} de Wompi. */
  private static final long TIMESTAMP_EVENTO = 1_791_000_000L;

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;
  @Autowired private EntityManager entityManager;

  @MockitoBean private ClienteWompi clienteWompi;

  private Usuario cliente;
  private Equipo equipo;
  private String tokenCliente;
  private String tokenOtroCliente;

  @BeforeEach
  void prepararDatos() {
    cliente = crearUsuario("cliente.wompi@gamespot.test");
    tokenCliente = "Bearer " + tokenDe(cliente);
    tokenOtroCliente = "Bearer " + tokenDe(crearUsuario("otro.wompi@gamespot.test"));
    equipo = equipoRepository.findAll().getFirst();
  }

  private Usuario crearUsuario(String email) {
    return usuarioRepository.saveAndFlush(
        Usuario.builder()
            .nombre("Cliente de prueba")
            .email(email)
            .passwordHash("hash-de-prueba")
            .rol(Rol.CLIENTE)
            .build());
  }

  private String tokenDe(Usuario usuario) {
    return jwtService.generarToken(usuarioDetailsService.loadUserByUsername(usuario.getEmail()));
  }

  private Reserva reserva(Instant expiraEn) {
    return reserva(expiraEn, LAS_TRES_PM);
  }

  private Reserva reserva(Instant expiraEn, Instant inicio) {
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(equipo)
            .inicio(inicio)
            .fin(inicio.plus(1, ChronoUnit.HOURS))
            .precioTotalPesos(ADELANTO_PESOS * 2)
            .adelantoPesos(ADELANTO_PESOS)
            .estado(EstadoReserva.PENDIENTE_PAGO)
            .expiraEn(expiraEn)
            .build());
  }

  private ResultActions iniciarCheckout(Reserva reserva, String token) throws Exception {
    return mockMvc.perform(
        post("/api/reservas/" + reserva.getId() + "/pago/wompi").header("Authorization", token));
  }

  /** Inicia el checkout y devuelve el pago PENDIENTE que quedó en la base. */
  private Pago pagoPendiente(Reserva reserva) throws Exception {
    iniciarCheckout(reserva, tokenCliente).andExpect(status().isCreated());
    return pagoRepository.findByReservaId(reserva.getId()).getFirst();
  }

  private static String sha256(String texto) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
  }

  /** Evento transaction.updated como lo envía Wompi, firmado con el secreto de eventos. */
  private static String evento(
      String idTransaccion, String referencia, String estado, long montoCentavos, String checksum)
      throws Exception {
    String firma =
        checksum != null
            ? checksum
            : sha256(idTransaccion + estado + montoCentavos + TIMESTAMP_EVENTO + SECRETO_EVENTOS);
    return """
        {
          "event": "transaction.updated",
          "data": {
            "transaction": {
              "id": "%s",
              "amount_in_cents": %d,
              "reference": "%s",
              "currency": "COP",
              "payment_method_type": "CARD",
              "status": "%s"
            }
          },
          "environment": "test",
          "signature": {
            "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"],
            "checksum": "%s"
          },
          "timestamp": %d,
          "sent_at": "2026-10-10T20:05:00.000Z"
        }
        """
        .formatted(idTransaccion, montoCentavos, referencia, estado, firma, TIMESTAMP_EVENTO);
  }

  private ResultActions enviarEvento(String cuerpo) throws Exception {
    return mockMvc.perform(
        post("/api/pagos/wompi/eventos").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
  }

  private Pago leer(Pago pago) {
    entityManager.flush();
    entityManager.clear();
    return pagoRepository.findById(pago.getId()).orElseThrow();
  }

  private Reserva leer(Reserva reserva) {
    return reservaRepository.findById(reserva.getId()).orElseThrow();
  }

  @Test
  @DisplayName("CP-32a El frontend sabe que el proveedor activo es Wompi")
  void informaElProveedorActivo() throws Exception {
    mockMvc
        .perform(get("/api/pagos/proveedor").header("Authorization", tokenCliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.proveedor").value("WOMPI"));
  }

  // Criterio 1: el checkout lleva el monto exacto del adelanto y la referencia, firmados.

  @Test
  @DisplayName(
      "CP-32b El checkout crea el pago PENDIENTE y firma el monto exacto y la referencia")
  void checkoutConMontoYReferenciaFirmados() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);

    MvcResult resultado =
        iniciarCheckout(pendiente, tokenCliente)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.montoCentavos").value(ADELANTO_CENTAVOS))
            .andExpect(jsonPath("$.moneda").value("COP"))
            .andExpect(jsonPath("$.llavePublica").value(LLAVE_PUBLICA))
            .andReturn();

    List<Pago> pagos = pagoRepository.findByReservaId(pendiente.getId());
    assertEquals(1, pagos.size());
    Pago pago = pagos.getFirst();
    assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    assertEquals(ADELANTO_CENTAVOS, pago.getMontoCentavos());
    assertTrue(pago.getReferencia().startsWith("GS-" + pendiente.getId() + "-"));

    String firmaEsperada =
        sha256(pago.getReferencia() + ADELANTO_CENTAVOS + "COP" + SECRETO_INTEGRIDAD);
    String url =
        URLDecoder.decode(
            com.jayway.jsonpath.JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.urlCheckout"),
            StandardCharsets.UTF_8);
    assertTrue(url.startsWith("https://checkout.wompi.co/p/?public-key=" + LLAVE_PUBLICA));
    assertTrue(url.contains("&amount-in-cents=" + ADELANTO_CENTAVOS));
    assertTrue(url.contains("&reference=" + pago.getReferencia()));
    assertTrue(url.contains("&signature:integrity=" + firmaEsperada));
    assertTrue(
        url.contains("&redirect-url=http://localhost:4200/reservas/" + pendiente.getId() + "/pagar"));
  }

  @Test
  @DisplayName("CP-32c El checkout aplica las reglas de CACS-5: 403 ajena y 409 plazo vencido")
  void checkoutRespetaLasReglasDelPago() throws Exception {
    iniciarCheckout(reserva(PLAZO_VIGENTE), tokenOtroCliente).andExpect(status().isForbidden());
    Reserva vencida = reserva(PLAZO_VENCIDO, LAS_TRES_PM.plus(2, ChronoUnit.HOURS));
    iniciarCheckout(vencida, tokenCliente).andExpect(status().isConflict());
  }

  // Criterio 2: un evento válido APPROVED aprueba el pago y confirma la reserva.

  @Test
  @DisplayName("CP-32d Un evento APPROVED con firma válida aprueba el pago y confirma la reserva")
  void eventoAprobadoConfirmaLaReserva() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);

    enviarEvento(evento("1234-1610641025-49201", pago.getReferencia(), "APPROVED",
            ADELANTO_CENTAVOS, null))
        .andExpect(status().isOk());

    Pago guardado = leer(pago);
    assertEquals(EstadoPago.APROBADO, guardado.getEstado());
    assertEquals("1234-1610641025-49201", guardado.getIdTransaccionWompi());
    assertEquals("CARD", guardado.getMetodoPago());
    assertNotNull(guardado.getRespuestaWompi());
    Reserva confirmada = leer(pendiente);
    assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
    assertTrue(confirmada.isTieneAnticipoPagado());
  }

  // Criterio 3: DECLINED o ERROR no confirman la reserva.

  @Test
  @DisplayName("CP-32e Un evento DECLINED rechaza el pago y la reserva sigue PENDIENTE_PAGO")
  void eventoRechazadoNoConfirma() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);

    enviarEvento(evento("tx-rechazada", pago.getReferencia(), "DECLINED", ADELANTO_CENTAVOS, null))
        .andExpect(status().isOk());

    assertEquals(EstadoPago.RECHAZADO, leer(pago).getEstado());
    Reserva sigue = leer(pendiente);
    assertEquals(EstadoReserva.PENDIENTE_PAGO, sigue.getEstado());
    assertFalse(sigue.isTieneAnticipoPagado());
  }

  @Test
  @DisplayName("CP-32f Un evento ERROR deja el pago en ERROR y la reserva sigue PENDIENTE_PAGO")
  void eventoErrorNoConfirma() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);

    enviarEvento(evento("tx-error", pago.getReferencia(), "ERROR", ADELANTO_CENTAVOS, null))
        .andExpect(status().isOk());

    assertEquals(EstadoPago.ERROR, leer(pago).getEstado());
    assertEquals(EstadoReserva.PENDIENTE_PAGO, leer(pendiente).getEstado());
  }

  // Criterio 4: un evento con firma inválida se rechaza y no cambia nada.

  @Test
  @DisplayName("CP-32g Un evento con checksum inválido responde 401 y el pago sigue PENDIENTE")
  void eventoConFirmaInvalidaSeRechaza() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);

    enviarEvento(evento("tx-falsa", pago.getReferencia(), "APPROVED", ADELANTO_CENTAVOS,
            sha256("checksum-inventado")))
        .andExpect(status().isUnauthorized());
    enviarEvento("{\"event\": \"transaction.updated\", \"data\": {}}")
        .andExpect(status().isUnauthorized());

    assertEquals(EstadoPago.PENDIENTE, leer(pago).getEstado());
    assertEquals(EstadoReserva.PENDIENTE_PAGO, leer(pendiente).getEstado());
  }

  // Criterio 5: un evento repetido no crea un segundo pago aprobado.

  @Test
  @DisplayName("CP-32h El mismo evento dos veces no duplica el pago y un evento tardío no lo revierte")
  void eventoRepetidoEsIdempotente() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);
    String aprobado =
        evento("tx-repetida", pago.getReferencia(), "APPROVED", ADELANTO_CENTAVOS, null);

    enviarEvento(aprobado).andExpect(status().isOk());
    enviarEvento(aprobado).andExpect(status().isOk());
    enviarEvento(evento("tx-repetida", pago.getReferencia(), "DECLINED", ADELANTO_CENTAVOS, null))
        .andExpect(status().isOk());

    List<Pago> pagos = pagoRepository.findByReservaId(pendiente.getId());
    assertEquals(1, pagos.size());
    assertEquals(EstadoPago.APROBADO, leer(pago).getEstado());
    assertEquals(EstadoReserva.CONFIRMADA, leer(pendiente).getEstado());
  }

  @Test
  @DisplayName("CP-32i Un evento con otro monto deja el pago en ERROR y no confirma la reserva")
  void eventoConOtroMontoNoConfirma() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);

    enviarEvento(evento("tx-monto", pago.getReferencia(), "APPROVED", 100L, null))
        .andExpect(status().isOk());

    assertEquals(EstadoPago.ERROR, leer(pago).getEstado());
    assertEquals(EstadoReserva.PENDIENTE_PAGO, leer(pendiente).getEstado());
  }

  @Test
  @DisplayName("CP-32j Al volver del checkout se consulta la transacción en Wompi y se registra")
  void consultaLaTransaccionAlVolver() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);
    when(clienteWompi.consultarTransaccion("tx-retorno"))
        .thenReturn(
            Optional.of(
                new TransaccionWompi(
                    "tx-retorno", pago.getReferencia(), "APPROVED", ADELANTO_CENTAVOS, "COP",
                    "NEQUI")));
    when(clienteWompi.consultarTransaccion("tx-inexistente")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/api/reservas/" + pendiente.getId() + "/pago/wompi/tx-retorno")
                .header("Authorization", tokenOtroCliente))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/reservas/" + pendiente.getId() + "/pago/wompi/tx-inexistente")
                .header("Authorization", tokenCliente))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/reservas/" + pendiente.getId() + "/pago/wompi/tx-retorno")
                .header("Authorization", tokenCliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoPago").value("APROBADO"))
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));

    assertEquals("NEQUI", leer(pago).getMetodoPago());
  }

  @Test
  @DisplayName("CP-32k Con Wompi activo, el pago simulado responde 409 y no crea pagos")
  void pagoSimuladoDesactivadoConWompi() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);

    mockMvc
        .perform(
            post("/api/reservas/" + pendiente.getId() + "/pago")
                .header("Authorization", tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultadoSimulado\": \"APROBADO\"}"))
        .andExpect(status().isConflict());

    assertTrue(pagoRepository.findByReservaId(pendiente.getId()).isEmpty());
  }

  // Criterio 6: ninguna llave de Wompi queda en el código ni en archivos versionados.

  @Test
  @DisplayName("CP-32l Sin llaves de Wompi en el código y sin arrancar con Wompi a medio configurar")
  void sinLlavesEnElCodigo() throws IOException {
    // Una llave real es el prefijo de Wompi seguido de su valor (por ejemplo pub_test_ y 32
    // caracteres). El prefijo solo, como en la documentación, no es una llave.
    Pattern llave =
        Pattern.compile("(pub|prv)_(test|prod)_[A-Za-z0-9]{8,}|(test|prod)_(events|integrity)_"
            + "[A-Za-z0-9]{8,}");
    for (String carpeta : List.of("src/main", "src/test/resources")) {
      try (Stream<Path> archivos = Files.walk(Path.of(carpeta))) {
        for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
          String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
          assertFalse(llave.matcher(contenido).find(), archivo + " contiene una llave de Wompi");
        }
      }
    }

    PropiedadesPagos.Wompi sinLlaves =
        new PropiedadesPagos.Wompi("", "", "", "https://sandbox.wompi.co/v1",
            "https://checkout.wompi.co/p/", "http://localhost:4200/reservas/{reservaId}/pagar");
    assertThrows(
        IllegalStateException.class, () -> new PropiedadesPagos(ProveedorPagos.WOMPI, sinLlaves));
    assertEquals(
        ProveedorPagos.SIMULADO, new PropiedadesPagos(null, sinLlaves).proveedor());
  }
}
