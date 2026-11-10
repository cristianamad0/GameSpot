package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.config.PropiedadesPagos;
import com.usta.gamespot.config.ProveedorPagos;
import com.usta.gamespot.dto.TransaccionWompi;
import com.usta.gamespot.exception.WompiNoRespondeException;
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
import jakarta.persistence.LockModeType;
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
import java.util.concurrent.atomic.AtomicReference;
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
    wompiConoceLaTransaccionDe(cuerpo);
    return mockMvc.perform(
        post("/api/pagos/wompi/eventos").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
  }

  /**
   * CACS-24: el webhook vuelve a leer en Wompi la transacción del evento (la referencia no va
   * firmada). Aquí el Wompi simulado conoce la transacción tal como viene en el evento.
   */
  private void wompiConoceLaTransaccionDe(String cuerpo) {
    Object transaccion;
    try {
      transaccion = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.data.transaction");
    } catch (com.jayway.jsonpath.PathNotFoundException ex) {
      return;
    }
    if (!(transaccion instanceof java.util.Map<?, ?> datos)) {
      return;
    }
    String id = String.valueOf(datos.get("id"));
    when(clienteWompi.consultarTransaccion(id))
        .thenReturn(
            Optional.of(
                new TransaccionWompi(
                    id,
                    String.valueOf(datos.get("reference")),
                    String.valueOf(datos.get("status")),
                    ((Number) datos.get("amount_in_cents")).longValue(),
                    String.valueOf(datos.get("currency")),
                    String.valueOf(datos.get("payment_method_type")))));
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

  // CACS-24: correcciones de la revisión del pago con Wompi.

  @Test
  @DisplayName("CP-36a Dos clics en pagar reutilizan el mismo pago PENDIENTE y la misma referencia")
  void checkoutRepetidoReutilizaElPago() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);

    String primera = referenciaDelCheckout(pendiente);
    String segunda = referenciaDelCheckout(pendiente);

    assertEquals(primera, segunda);
    assertEquals(1, pagoRepository.findByReservaId(pendiente.getId()).size());
  }

  private String referenciaDelCheckout(Reserva reserva) throws Exception {
    MvcResult resultado =
        iniciarCheckout(reserva, tokenCliente).andExpect(status().isCreated()).andReturn();
    return com.jayway.jsonpath.JsonPath.read(
        resultado.getResponse().getContentAsString(), "$.referencia");
  }

  @Test
  @DisplayName(
      "CP-36b Un segundo pago aprobado de una reserva ya confirmada queda en ERROR, sin error 500")
  void segundoPagoAprobadoQuedaEnError() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago primero = pagoPendiente(pendiente);
    // Un segundo checkout abierto antes de esta corrección, con otra referencia.
    Pago segundo =
        pagoRepository.saveAndFlush(
            Pago.builder()
                .reserva(pendiente)
                .referencia("GS-" + pendiente.getId() + "-segundo")
                .montoCentavos(ADELANTO_CENTAVOS)
                .moneda("COP")
                .build());

    enviarEvento(evento("tx-primero", primero.getReferencia(), "APPROVED", ADELANTO_CENTAVOS,
            null))
        .andExpect(status().isOk());
    enviarEvento(evento("tx-segundo", segundo.getReferencia(), "APPROVED", ADELANTO_CENTAVOS,
            null))
        .andExpect(status().isOk());

    assertEquals(EstadoPago.APROBADO, leer(primero).getEstado());
    Pago duplicado = leer(segundo);
    assertEquals(EstadoPago.ERROR, duplicado.getEstado());
    assertEquals("tx-segundo", duplicado.getIdTransaccionWompi());
    assertEquals(EstadoReserva.CONFIRMADA, leer(pendiente).getEstado());
  }

  @Test
  @DisplayName(
      "CP-36c Un evento firmado con la referencia cambiada no confirma la reserva de otro")
  void referenciaCambiadaNoConfirmaOtraReserva() throws Exception {
    Reserva propia = reserva(PLAZO_VIGENTE);
    Pago pagoPropio = pagoPendiente(propia);
    Reserva ajena = reserva(PLAZO_VIGENTE, LAS_TRES_PM.plus(2, ChronoUnit.HOURS));
    Pago pagoAjeno = pagoPendiente(ajena);
    // En Wompi la transacción aprobada es la del pago propio.
    when(clienteWompi.consultarTransaccion("tx-propia"))
        .thenReturn(
            Optional.of(
                new TransaccionWompi(
                    "tx-propia", pagoPropio.getReferencia(), "APPROVED", ADELANTO_CENTAVOS,
                    "COP", "CARD")));

    // La referencia no va firmada: se cambia por la del otro pago y el checksum sigue válido.
    mockMvc
        .perform(
            post("/api/pagos/wompi/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    evento("tx-propia", pagoAjeno.getReferencia(), "APPROVED",
                        ADELANTO_CENTAVOS, null)))
        .andExpect(status().isOk());

    assertEquals(EstadoPago.PENDIENTE, leer(pagoAjeno).getEstado());
    assertEquals(EstadoReserva.PENDIENTE_PAGO, leer(ajena).getEstado());
    assertEquals(EstadoPago.APROBADO, leer(pagoPropio).getEstado());
  }

  @Test
  @DisplayName("CP-36d Al volver del checkout, la reserva no está bloqueada mientras se consulta Wompi")
  void noBloqueaLaReservaDuranteLaConsulta() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    // El pago se crea directo en la base: el checkout bloquearía la reserva en esta misma
    // transacción de prueba y no se vería si la consulta la bloquea antes de tiempo.
    Pago pago =
        pagoRepository.saveAndFlush(
            Pago.builder()
                .reserva(pendiente)
                .referencia("GS-" + pendiente.getId() + "-retorno")
                .montoCentavos(ADELANTO_CENTAVOS)
                .moneda("COP")
                .build());
    AtomicReference<LockModeType> bloqueoDuranteLaConsulta = new AtomicReference<>();
    when(clienteWompi.consultarTransaccion("tx-sin-bloqueo"))
        .thenAnswer(
            invocacion -> {
              bloqueoDuranteLaConsulta.set(entityManager.getLockMode(leer(pendiente)));
              return Optional.of(
                  new TransaccionWompi(
                      "tx-sin-bloqueo", pago.getReferencia(), "APPROVED", ADELANTO_CENTAVOS,
                      "COP", "CARD"));
            });
    // Hibernate marca como escritas las filas insertadas en la transacción; se limpia el contexto
    // para que la consulta lea la reserva desde cero, como en una petición real.
    entityManager.flush();
    entityManager.clear();

    mockMvc
        .perform(
            get("/api/reservas/" + pendiente.getId() + "/pago/wompi/tx-sin-bloqueo")
                .header("Authorization", tokenCliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));

    assertNotNull(bloqueoDuranteLaConsulta.get());
    assertFalse(
        esBloqueo(bloqueoDuranteLaConsulta.get()),
        "La reserva estaba bloqueada mientras se esperaba a Wompi");
    // Después de la consulta sí se bloquea, para registrar el pago.
    assertTrue(esBloqueo(entityManager.getLockMode(leer(pendiente))));
  }

  private static boolean esBloqueo(LockModeType modo) {
    return modo.name().startsWith("PESSIMISTIC");
  }

  @Test
  @DisplayName("CP-36e Si Wompi no responde, la consulta da 503 con un mensaje claro")
  void wompiCaidoResponde503() throws Exception {
    Reserva pendiente = reserva(PLAZO_VIGENTE);
    Pago pago = pagoPendiente(pendiente);
    when(clienteWompi.consultarTransaccion(anyString()))
        .thenThrow(new WompiNoRespondeException(new RuntimeException("tiempo agotado")));

    mockMvc
        .perform(
            get("/api/reservas/" + pendiente.getId() + "/pago/wompi/tx-lenta")
                .header("Authorization", tokenCliente))
        .andExpect(status().isServiceUnavailable())
        .andExpect(
            jsonPath("$.mensaje")
                .value("Wompi no respondió. Si ya pagaste, consulta de nuevo en unos segundos"));

    assertEquals(EstadoPago.PENDIENTE, leer(pago).getEstado());
  }
}
