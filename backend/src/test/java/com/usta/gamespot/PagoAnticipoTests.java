package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.usta.gamespot.service.ProveedorAnticiposSimulado;
import com.usta.gamespot.service.UsuarioDetailsService;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del pago del adelanto con el proveedor simulado (HU-05, CACS-5). Llaman al endpoint con
 * un token JWT real. Mientras CACS-4 no exista, las reservas se crean directamente en la base. Cada
 * prueba corre en una transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class PagoAnticipoTests {

  /** 10 de octubre de 2026, 15:00 en Bogotá (UTC-5). */
  private static final Instant LAS_TRES_PM = Instant.parse("2026-10-10T20:00:00Z");

  /** Plazo de pago que todavía no vence cuando corren las pruebas. */
  private static final Instant PLAZO_VIGENTE = Instant.parse("2099-12-31T05:00:00Z");

  /** Plazo de pago ya vencido: 10 de octubre de 2026, 14:00 en Bogotá. */
  private static final Instant PLAZO_VENCIDO = Instant.parse("2026-10-10T19:00:00Z");

  /** Adelanto de una reserva de una hora en estas pruebas: $2.500 = 250.000 centavos. */
  private static final int ADELANTO_PESOS = 2500;

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;
  @Autowired private EntityManager entityManager;

  private Usuario cliente;
  private Equipo equipo;
  private String tokenCliente;
  private String tokenOtroCliente;

  @BeforeEach
  void prepararDatos() {
    cliente = crearUsuario("cliente@gamespot.test");
    tokenCliente = "Bearer " + tokenDe(cliente);
    tokenOtroCliente = "Bearer " + tokenDe(crearUsuario("otro@gamespot.test"));
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

  /** Reserva de una hora del cliente en el estado indicado, con los datos que exige la base. */
  private Reserva reserva(EstadoReserva estado, Instant expiraEn) {
    boolean estaPagada = estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA;
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(equipo)
            .inicio(LAS_TRES_PM)
            .fin(LAS_TRES_PM.plus(1, ChronoUnit.HOURS))
            .precioTotalPesos(ADELANTO_PESOS * 2)
            .adelantoPesos(ADELANTO_PESOS)
            .estado(estado)
            .tieneAnticipoPagado(estaPagada)
            .expiraEn(expiraEn)
            .canceladaEn(estado == EstadoReserva.CANCELADA ? LAS_TRES_PM : null)
            .build());
  }

  private Reserva reservaPendiente() {
    return reserva(EstadoReserva.PENDIENTE_PAGO, PLAZO_VIGENTE);
  }

  private ResultActions pagar(Reserva reserva, String token, String cuerpo) throws Exception {
    return mockMvc.perform(
        post("/api/reservas/" + reserva.getId() + "/pago")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }

  private ResultActions pagarConResultado(Reserva reserva, String resultado) throws Exception {
    return pagar(reserva, tokenCliente, "{\"resultadoSimulado\": \"" + resultado + "\"}");
  }

  // Criterio 1: al aprobarse el pago la reserva queda confirmada.

  @Test
  @DisplayName("CP-23a Con el pago aprobado, la reserva queda CONFIRMADA, pagada y sin expiración")
  void pagoAprobadoConfirmaLaReserva() throws Exception {
    Reserva pendiente = reservaPendiente();

    pagarConResultado(pendiente, "APROBADO")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoPago").value("APROBADO"))
        .andExpect(jsonPath("$.montoCentavos").value(ADELANTO_PESOS * 100))
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"))
        .andExpect(jsonPath("$.tieneAnticipoPagado").value(true))
        .andExpect(jsonPath("$.expiraEn").doesNotExist());

    entityManager.refresh(pendiente);
    assertEquals(EstadoReserva.CONFIRMADA, pendiente.getEstado());
    assertTrue(pendiente.isTieneAnticipoPagado());
    assertNull(pendiente.getExpiraEn());

    List<Pago> pagos = pagoRepository.findByReservaId(pendiente.getId());
    assertEquals(1, pagos.size());
    assertEquals(EstadoPago.APROBADO, pagos.getFirst().getEstado());
    assertEquals(ADELANTO_PESOS * 100L, pagos.getFirst().getMontoCentavos());
    assertTrue(pagos.getFirst().getReferencia().startsWith("GS-" + pendiente.getId() + "-"));
  }

  @Test
  @DisplayName("CP-23b Sin cuerpo en la petición, el proveedor simulado aprueba el pago")
  void sinCuerpoElPagoSeAprueba() throws Exception {
    Reserva pendiente = reservaPendiente();

    mockMvc
        .perform(
            post("/api/reservas/" + pendiente.getId() + "/pago")
                .header("Authorization", tokenCliente))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoPago").value("APROBADO"))
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));
  }

  // Criterio 2: un pago rechazado deja la reserva pendiente y se puede reintentar.

  @Test
  @DisplayName("CP-23c Con el pago rechazado la reserva sigue PENDIENTE_PAGO y se puede reintentar")
  void pagoRechazadoPermiteReintentar() throws Exception {
    Reserva pendiente = reservaPendiente();

    pagarConResultado(pendiente, "RECHAZADO")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoPago").value("RECHAZADO"))
        .andExpect(jsonPath("$.estadoReserva").value("PENDIENTE_PAGO"))
        .andExpect(jsonPath("$.tieneAnticipoPagado").value(false))
        .andExpect(jsonPath("$.expiraEn").exists());

    entityManager.refresh(pendiente);
    assertEquals(EstadoReserva.PENDIENTE_PAGO, pendiente.getEstado());
    assertFalse(pendiente.isTieneAnticipoPagado());

    pagarConResultado(pendiente, "APROBADO")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));

    List<EstadoPago> estados =
        pagoRepository.findByReservaId(pendiente.getId()).stream().map(Pago::getEstado).toList();
    assertEquals(2, estados.size());
    assertTrue(estados.containsAll(List.of(EstadoPago.RECHAZADO, EstadoPago.APROBADO)));
  }

  // Criterio 3: una reserva no tiene dos pagos aprobados.

  @Test
  @DisplayName("CP-23d Pagar otra vez una reserva ya pagada responde 409 y no crea otro pago")
  void segundoPagoResponde409() throws Exception {
    Reserva pendiente = reservaPendiente();
    pagarConResultado(pendiente, "APROBADO").andExpect(status().isCreated());

    pagarConResultado(pendiente, "APROBADO")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));

    List<Pago> pagos = pagoRepository.findByReservaId(pendiente.getId());
    assertEquals(1, pagos.size());
  }

  @Test
  @DisplayName("CP-23e La base de datos rechaza un segundo pago APROBADO para la misma reserva")
  void baseRechazaDosPagosAprobados() throws Exception {
    Reserva pendiente = reservaPendiente();
    pagarConResultado(pendiente, "APROBADO").andExpect(status().isCreated());

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            pagoRepository.saveAndFlush(
                Pago.builder()
                    .reserva(pendiente)
                    .referencia("REF-PRUEBA-SEGUNDO-APROBADO")
                    .montoCentavos(ADELANTO_PESOS * 100L)
                    .estado(EstadoPago.APROBADO)
                    .build()));
  }

  // Criterio 4: no se paga una reserva expirada, cancelada, vencida o ajena.

  @Test
  @DisplayName("CP-23f No se puede pagar una reserva EXPIRADA o CANCELADA (409) y no se crea pago")
  void reservaExpiradaOCanceladaResponde409() throws Exception {
    Reserva expirada = reserva(EstadoReserva.EXPIRADA, PLAZO_VENCIDO);
    pagarConResultado(expirada, "APROBADO").andExpect(status().isConflict());
    assertTrue(pagoRepository.findByReservaId(expirada.getId()).isEmpty());

    // La expirada ya no ocupa el bloque, así que se puede crear la cancelada en el mismo horario.
    Reserva cancelada = reserva(EstadoReserva.CANCELADA, null);
    pagarConResultado(cancelada, "APROBADO").andExpect(status().isConflict());
    assertTrue(pagoRepository.findByReservaId(cancelada.getId()).isEmpty());
  }

  @Test
  @DisplayName("CP-23g No se puede pagar una reserva pendiente cuyo plazo ya venció (409)")
  void plazoVencidoResponde409() throws Exception {
    Reserva vencida = reserva(EstadoReserva.PENDIENTE_PAGO, PLAZO_VENCIDO);

    pagarConResultado(vencida, "APROBADO")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("El plazo para pagar la reserva "
            + vencida.getId() + " ya venció"));

    entityManager.refresh(vencida);
    assertEquals(EstadoReserva.PENDIENTE_PAGO, vencida.getEstado());
    assertTrue(pagoRepository.findByReservaId(vencida.getId()).isEmpty());
  }

  @Test
  @DisplayName("CP-23h Un usuario no puede pagar la reserva de otro (403)")
  void reservaAjenaResponde403() throws Exception {
    Reserva pendiente = reservaPendiente();

    pagar(pendiente, tokenOtroCliente, "{\"resultadoSimulado\": \"APROBADO\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));

    entityManager.refresh(pendiente);
    assertEquals(EstadoReserva.PENDIENTE_PAGO, pendiente.getEstado());
    assertTrue(pagoRepository.findByReservaId(pendiente.getId()).isEmpty());
  }

  // Criterio 5: un pago menor al adelanto no confirma la reserva.

  @Test
  @DisplayName("CP-23i Un pago aprobado por menos del adelanto no confirma la reserva")
  void pagoMenorNoConfirma() {
    Reserva pendiente = reservaPendiente();

    pagoRepository.saveAndFlush(
        Pago.builder()
            .reserva(pendiente)
            .referencia("REF-PRUEBA-PAGO-INCOMPLETO")
            .montoCentavos(ADELANTO_PESOS * 100L - 1)
            .estado(EstadoPago.APROBADO)
            .build());

    entityManager.refresh(pendiente);
    assertEquals(EstadoReserva.PENDIENTE_PAGO, pendiente.getEstado());
    assertFalse(pendiente.isTieneAnticipoPagado());
  }

  // Criterio 6: ni llaves ni datos de tarjeta en el código o en la base.

  @Test
  @DisplayName("CP-23j El pago no guarda datos de tarjeta aunque el cliente los envíe")
  void noGuardaDatosDeTarjeta() throws Exception {
    Reserva pendiente = reservaPendiente();

    pagar(
            pendiente,
            tokenCliente,
            "{\"resultadoSimulado\": \"APROBADO\", \"numeroTarjeta\": \"4242424242424242\","
                + " \"cvc\": \"123\"}")
        .andExpect(status().isCreated());

    Pago pago = pagoRepository.findByReservaId(pendiente.getId()).getFirst();
    assertEquals(ProveedorAnticiposSimulado.METODO_SIMULADO, pago.getMetodoPago());
    assertNull(pago.getRespuestaWompi());
    assertFalse(pago.getReferencia().contains("4242"));

    List<String> camposSensibles = List.of("tarjeta", "card", "cvc", "cvv", "llave", "secret");
    List<String> camposDelPago =
        Arrays.stream(Pago.class.getDeclaredFields())
            .map(Field::getName)
            .map(nombre -> nombre.toLowerCase(Locale.ROOT))
            .toList();
    for (String campo : camposDelPago) {
      assertTrue(
          camposSensibles.stream().noneMatch(campo::contains),
          "La entidad Pago no debe tener el campo " + campo);
    }
  }

  // Alcance: solo usuarios autenticados y reservas existentes.

  @Test
  @DisplayName("CP-23k Sin token responde 401 y una reserva que no existe responde 404")
  void sinTokenYReservaInexistente() throws Exception {
    Reserva pendiente = reservaPendiente();

    mockMvc
        .perform(post("/api/reservas/" + pendiente.getId() + "/pago"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/reservas/999999/pago").header("Authorization", tokenCliente))
        .andExpect(status().isNotFound());
  }
}
