package com.usta.gamespot;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.MovimientoPuntosRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import com.usta.gamespot.service.UsuarioDetailsService;
import jakarta.persistence.EntityManager;
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
 * Pruebas de los puntos de fidelización (HU-07, CACS-7): saldo e historial del cliente y ajustes
 * manuales del administrador. Llaman a los endpoints con tokens JWT reales. La configuración del
 * negocio da 1 punto por hora jugada. Cada prueba corre en una transacción que se revierte.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class PuntosFidelidadTests {

  /** 10 de octubre de 2026, 15:00 en Bogotá (UTC-5). */
  private static final Instant LAS_TRES_PM = Instant.parse("2026-10-10T20:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private MovimientoPuntosRepository movimientoPuntosRepository;
  @Autowired private EntityManager entityManager;

  private Usuario cliente;
  private String tokenCliente;
  private String tokenAdmin;

  @BeforeEach
  void prepararDatos() {
    cliente = crearUsuario("cliente@gamespot.test", Rol.CLIENTE);
    tokenCliente = "Bearer " + tokenDe(cliente);
    tokenAdmin = "Bearer " + tokenDe(crearUsuario("admin@gamespot.test", Rol.ADMIN));
  }

  private Usuario crearUsuario(String email, Rol rol) {
    return usuarioRepository.saveAndFlush(
        Usuario.builder()
            .nombre("Usuario " + rol)
            .email(email)
            .passwordHash("hash-de-prueba")
            .rol(rol)
            .build());
  }

  private String tokenDe(Usuario usuario) {
    return jwtService.generarToken(usuarioDetailsService.loadUserByUsername(usuario.getEmail()));
  }

  /** Reserva CONFIRMADA del cliente que dura {@code minutos} minutos. */
  private Reserva reservaConfirmada(int minutos) {
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(equipoRepository.findAll().getFirst())
            .inicio(LAS_TRES_PM)
            .fin(LAS_TRES_PM.plus(minutos, ChronoUnit.MINUTES))
            .precioTotalPesos(5000)
            .adelantoPesos(2500)
            .estado(EstadoReserva.CONFIRMADA)
            .tieneAnticipoPagado(true)
            .build());
  }

  private ResultActions completar(Reserva reserva) throws Exception {
    return mockMvc.perform(
        post("/api/admin/reservas/" + reserva.getId() + "/completar")
            .header("Authorization", tokenAdmin));
  }

  private ResultActions consultarPuntos(String token) throws Exception {
    return mockMvc.perform(get("/api/puntos").header("Authorization", token));
  }

  private ResultActions ajustar(Long usuarioId, String token, String cuerpo) throws Exception {
    return mockMvc.perform(
        post("/api/admin/usuarios/" + usuarioId + "/puntos")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }

  private ResultActions ajustar(int puntos, String descripcion) throws Exception {
    return ajustar(
        cliente.getId(),
        tokenAdmin,
        "{\"puntos\": " + puntos + ", \"descripcion\": \"" + descripcion + "\"}");
  }

  private int saldoDelCliente() {
    entityManager.refresh(cliente);
    return cliente.getPuntosFidelidad();
  }

  // Criterio 1: completar una reserva suma puntos proporcionales a los minutos jugados.

  @Test
  @DisplayName("CP-24a Al completar una reserva de 150 minutos, el cliente suma 2 puntos y los ve")
  void completarSumaPuntosProporcionales() throws Exception {
    Reserva reserva = reservaConfirmada(150);

    completar(reserva).andExpect(status().isOk());

    // 150 minutos x 1 punto por hora = 2,5 puntos: se otorgan los puntos completos (2).
    consultarPuntos(tokenCliente)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoPuntos").value(2))
        .andExpect(jsonPath("$.movimientos", hasSize(1)))
        .andExpect(jsonPath("$.movimientos[0].puntos").value(2))
        .andExpect(jsonPath("$.movimientos[0].tipo").value("HORAS_JUGADAS"))
        .andExpect(jsonPath("$.movimientos[0].descripcion").value("150 minutos jugados"))
        .andExpect(jsonPath("$.movimientos[0].reservaId").value(reserva.getId()));
  }

  // Criterio 2: los puntos de una reserva se otorgan una sola vez.

  @Test
  @DisplayName("CP-24b Completar otra vez la misma reserva no vuelve a sumar puntos")
  void puntosDeUnaReservaUnaSolaVez() throws Exception {
    Reserva reserva = reservaConfirmada(120);

    completar(reserva).andExpect(status().isOk());
    completar(reserva).andExpect(status().isConflict());

    consultarPuntos(tokenCliente)
        .andExpect(jsonPath("$.saldoPuntos").value(2))
        .andExpect(jsonPath("$.movimientos", hasSize(1)));
  }

  // Criterio 3: el cliente consulta su saldo y su historial.

  @Test
  @DisplayName("CP-24c El cliente ve su saldo y su historial, del movimiento más reciente al más antiguo")
  void clienteVeSaldoEHistorial() throws Exception {
    completar(reservaConfirmada(60)).andExpect(status().isOk());
    ajustar(10, "Bono de bienvenida").andExpect(status().isCreated());
    ajustar(-4, "Canje de bebida").andExpect(status().isCreated());

    consultarPuntos(tokenCliente)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuarioId").value(cliente.getId()))
        .andExpect(jsonPath("$.saldoPuntos").value(7))
        .andExpect(jsonPath("$.movimientos", hasSize(3)))
        .andExpect(jsonPath("$.movimientos[0].puntos").value(-4))
        .andExpect(jsonPath("$.movimientos[0].tipo").value("AJUSTE_ADMIN"))
        .andExpect(jsonPath("$.movimientos[0].descripcion").value("Canje de bebida"))
        .andExpect(jsonPath("$.movimientos[0].fecha").exists())
        .andExpect(jsonPath("$.movimientos[1].descripcion").value("Bono de bienvenida"))
        .andExpect(jsonPath("$.movimientos[2].tipo").value("HORAS_JUGADAS"));
  }

  @Test
  @DisplayName("CP-24d Cada cliente ve solo sus puntos; uno sin movimientos recibe saldo 0 y lista vacía")
  void clienteSoloVeSusPuntos() throws Exception {
    ajustar(10, "Bono de bienvenida").andExpect(status().isCreated());
    Usuario otro = crearUsuario("otro@gamespot.test", Rol.CLIENTE);

    consultarPuntos("Bearer " + tokenDe(otro))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuarioId").value(otro.getId()))
        .andExpect(jsonPath("$.saldoPuntos").value(0))
        .andExpect(jsonPath("$.movimientos", hasSize(0)));
  }

  @Test
  @DisplayName("CP-24e Sin token, consultar los puntos responde 401")
  void consultarSinTokenResponde401() throws Exception {
    mockMvc.perform(get("/api/puntos")).andExpect(status().isUnauthorized());
  }

  // Criterio 4: el administrador registra ajustes con descripción obligatoria.

  @Test
  @DisplayName("CP-24f El admin registra un ajuste manual y el saldo del cliente cambia")
  void adminRegistraAjuste() throws Exception {
    ajustar(5, "  Compensación por falla del equipo  ")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.usuarioId").value(cliente.getId()))
        .andExpect(jsonPath("$.saldoPuntos").value(5))
        .andExpect(jsonPath("$.movimientos[0].puntos").value(5))
        .andExpect(jsonPath("$.movimientos[0].tipo").value("AJUSTE_ADMIN"))
        .andExpect(jsonPath("$.movimientos[0].descripcion")
            .value("Compensación por falla del equipo"))
        .andExpect(jsonPath("$.movimientos[0].reservaId").doesNotExist());

    assertEquals(5, saldoDelCliente());
  }

  @Test
  @DisplayName("CP-24g Un ajuste sin descripción, con cero puntos o sin puntos responde 400")
  void ajusteInvalidoResponde400() throws Exception {
    ajustar(5, "   ").andExpect(status().isBadRequest());
    ajustar(0, "Ajuste vacío").andExpect(status().isBadRequest());
    ajustar(cliente.getId(), tokenAdmin, "{\"descripcion\": \"Sin puntos\"}")
        .andExpect(status().isBadRequest());

    assertTrue(movimientoPuntosRepository.findByUsuarioIdOrderByCreadoEnDesc(cliente.getId())
        .isEmpty());
    assertEquals(0, saldoDelCliente());
  }

  @Test
  @DisplayName("CP-24h Un CLIENTE no puede ajustar puntos (403) y un usuario inexistente da 404")
  void ajusteSinPermisoOUsuarioInexistente() throws Exception {
    ajustar(cliente.getId(), tokenCliente, "{\"puntos\": 100, \"descripcion\": \"Intento\"}")
        .andExpect(status().isForbidden());
    ajustar(999999L, tokenAdmin, "{\"puntos\": 5, \"descripcion\": \"No existe\"}")
        .andExpect(status().isNotFound());

    assertEquals(0, saldoDelCliente());
  }

  // Criterio 5: ningún ajuste deja el saldo en negativo.

  @Test
  @DisplayName("CP-24i Un ajuste que deja el saldo negativo responde 409; dejarlo en 0 sí se permite")
  void ajusteNoDejaSaldoNegativo() throws Exception {
    ajustar(3, "Bono").andExpect(status().isCreated());

    ajustar(-4, "Canje mayor al saldo")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
    assertEquals(3, saldoDelCliente());

    ajustar(-3, "Canje de todo el saldo")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.saldoPuntos").value(0));
  }
}
