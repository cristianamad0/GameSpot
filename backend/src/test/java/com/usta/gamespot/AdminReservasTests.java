package com.usta.gamespot;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.MovimientoPuntos;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.TipoMovimientoPuntos;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.MovimientoPuntosRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import com.usta.gamespot.service.UsuarioDetailsService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del panel de administración de reservas (HU-06, CACS-6). Llaman a los endpoints con un
 * token JWT real, así que también prueban las reglas de acceso de SecurityConfig. Cada prueba
 * corre en una transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class AdminReservasTests {

  /** 10 de octubre de 2026, 15:00 en Bogotá (UTC-5). */
  private static final Instant LAS_TRES_PM = Instant.parse("2026-10-10T20:00:00Z");

  /** 10 de octubre de 2026, 23:30 en Bogotá: sigue siendo el mismo día del negocio. */
  private static final Instant ONCE_Y_MEDIA_PM = Instant.parse("2026-10-11T04:30:00Z");

  /** 11 de octubre de 2026, 00:00 en Bogotá: ya es el día siguiente. */
  private static final Instant MEDIANOCHE = Instant.parse("2026-10-11T05:00:00Z");

  private static final String RUTA = "/api/admin/reservas";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;
  @Autowired private MovimientoPuntosRepository movimientoPuntosRepository;
  @Autowired private EntityManager entityManager;

  private Usuario cliente;
  private Equipo equipoUno;
  private Equipo equipoDos;
  private String tokenAdmin;
  private String tokenCliente;

  @BeforeEach
  void prepararDatos() {
    cliente = crearUsuario("cliente@gamespot.test", Rol.CLIENTE);
    Usuario admin = crearUsuario("admin@gamespot.test", Rol.ADMIN);
    tokenCliente = "Bearer " + tokenDe(cliente);
    tokenAdmin = "Bearer " + tokenDe(admin);

    List<Equipo> equipos = equipoRepository.findAll();
    equipoUno = equipos.get(0);
    equipoDos = equipos.get(1);
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

  /** Reserva de {@code horas} horas en el estado indicado, con los datos que exige la base. */
  private Reserva reserva(Equipo equipo, Instant inicio, int horas, EstadoReserva estado) {
    boolean estaPagada = estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA;
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(equipo)
            .inicio(inicio)
            .fin(inicio.plus(horas, ChronoUnit.HOURS))
            .precioTotalPesos(5000 * horas)
            .adelantoPesos(2500 * horas)
            .estado(estado)
            .tieneAnticipoPagado(estaPagada)
            .expiraEn(estado == EstadoReserva.PENDIENTE_PAGO ? inicio : null)
            .canceladaEn(estado == EstadoReserva.CANCELADA ? inicio : null)
            .build());
  }

  private String cuerpoConMotivo(String motivo) {
    return "{\"motivo\": \"" + motivo + "\"}";
  }

  // Criterio 1: solo ADMIN entra al panel.

  @Test
  @DisplayName("CP-22a Un CLIENTE recibe 403 en las rutas /api/admin/**")
  void clienteRecibe403() throws Exception {
    mockMvc
        .perform(get(RUTA).param("fecha", "2026-10-10").header("Authorization", tokenCliente))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post(RUTA + "/1/completar").header("Authorization", tokenCliente))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CP-22b Sin token, las rutas /api/admin/** responden 401")
  void sinTokenRecibe401() throws Exception {
    mockMvc.perform(get(RUTA).param("fecha", "2026-10-10")).andExpect(status().isUnauthorized());
    mockMvc.perform(post(RUTA + "/1/completar")).andExpect(status().isUnauthorized());
  }

  // Criterio 2: listar las reservas del día con filtros.

  @Test
  @DisplayName("CP-22c El admin lista las reservas de un día (hora de Bogotá) y filtra por equipo y estado")
  void listaReservasDelDiaConFiltros() throws Exception {
    Reserva confirmadaUno = reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.CONFIRMADA);
    reserva(equipoUno, ONCE_Y_MEDIA_PM, 1, EstadoReserva.PENDIENTE_PAGO);
    Reserva confirmadaDos = reserva(equipoDos, LAS_TRES_PM, 1, EstadoReserva.CONFIRMADA);
    reserva(equipoDos, MEDIANOCHE, 1, EstadoReserva.CONFIRMADA); // día siguiente

    mockMvc
        .perform(get(RUTA).param("fecha", "2026-10-10").header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)));

    mockMvc
        .perform(
            get(RUTA)
                .param("fecha", "2026-10-10")
                .param("equipoId", equipoDos.getId().toString())
                .header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(confirmadaDos.getId()))
        .andExpect(jsonPath("$[0].nombreEquipo").value(equipoDos.getNombre()))
        .andExpect(jsonPath("$[0].emailCliente").value(cliente.getEmail()));

    mockMvc
        .perform(
            get(RUTA)
                .param("fecha", "2026-10-10")
                .param("equipoId", equipoUno.getId().toString())
                .param("estado", "CONFIRMADA")
                .header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(confirmadaUno.getId()))
        .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
  }

  // Criterio 3: cancelar deja la reserva CANCELADA y libera el bloque.

  @Test
  @DisplayName("CP-22d Al cancelar, la reserva queda CANCELADA con fecha y motivo, y su bloque queda libre")
  void cancelarLiberaElBloque() throws Exception {
    Reserva confirmada = reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.CONFIRMADA);

    mockMvc
        .perform(
            post(RUTA + "/" + confirmada.getId() + "/cancelar")
                .header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConMotivo("Falla eléctrica en el local")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADA"))
        .andExpect(jsonPath("$.motivoCancelacion").value("Falla eléctrica en el local"));

    entityManager.refresh(confirmada);
    assertEquals(EstadoReserva.CANCELADA, confirmada.getEstado());
    assertNotNull(confirmada.getCanceladaEn());
    assertEquals("Falla eléctrica en el local", confirmada.getMotivoCancelacion());
    assertDoesNotThrow(
        () -> reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.PENDIENTE_PAGO));
  }

  @Test
  @DisplayName("CP-22e También se puede cancelar una reserva PENDIENTE_PAGO")
  void cancelarReservaPendiente() throws Exception {
    Reserva pendiente = reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.PENDIENTE_PAGO);

    mockMvc
        .perform(
            post(RUTA + "/" + pendiente.getId() + "/cancelar")
                .header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConMotivo("El cliente pidió cancelar")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADA"));
  }

  // Criterio 4: no se cancela sin motivo ni en estados finales.

  @Test
  @DisplayName("CP-22f No se puede cancelar sin motivo (400) y la reserva no cambia")
  void cancelarSinMotivoFalla() throws Exception {
    Reserva confirmada = reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.CONFIRMADA);

    mockMvc
        .perform(
            post(RUTA + "/" + confirmada.getId() + "/cancelar")
                .header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConMotivo("   ")))
        .andExpect(status().isBadRequest());

    entityManager.refresh(confirmada);
    assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
  }

  @Test
  @DisplayName("CP-22g No se puede cancelar una reserva COMPLETADA, EXPIRADA o CANCELADA (409)")
  void cancelarEstadoFinalFalla() throws Exception {
    List<Reserva> finales =
        List.of(
            reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.COMPLETADA),
            reserva(equipoDos, LAS_TRES_PM, 1, EstadoReserva.EXPIRADA),
            reserva(equipoUno, MEDIANOCHE, 1, EstadoReserva.CANCELADA));

    for (Reserva reserva : finales) {
      EstadoReserva estadoAntes = reserva.getEstado();
      mockMvc
          .perform(
              post(RUTA + "/" + reserva.getId() + "/cancelar")
                  .header("Authorization", tokenAdmin)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(cuerpoConMotivo("Intento de prueba")))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409));

      entityManager.refresh(reserva);
      assertEquals(estadoAntes, reserva.getEstado());
    }
  }

  // Criterio 5: completar otorga los puntos una sola vez.

  @Test
  @DisplayName("CP-22h Al completar, el cliente recibe sus puntos una sola vez")
  void completarOtorgaPuntosUnaVez() throws Exception {
    Reserva confirmada = reserva(equipoUno, LAS_TRES_PM, 2, EstadoReserva.CONFIRMADA);
    String rutaCompletar = RUTA + "/" + confirmada.getId() + "/completar";

    mockMvc
        .perform(post(rutaCompletar).header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("COMPLETADA"));
    // Un segundo intento no vuelve a sumar puntos.
    mockMvc
        .perform(post(rutaCompletar).header("Authorization", tokenAdmin))
        .andExpect(status().isConflict());

    entityManager.refresh(cliente);
    List<MovimientoPuntos> movimientos =
        movimientoPuntosRepository.findByUsuarioIdOrderByCreadoEnDesc(cliente.getId());
    // 2 horas jugadas x 1 punto por hora (configuración del negocio).
    assertEquals(2, cliente.getPuntosFidelidad());
    assertEquals(1, movimientos.size());
    assertEquals(TipoMovimientoPuntos.HORAS_JUGADAS, movimientos.getFirst().getTipo());
  }

  // Criterio 6: solo una CONFIRMADA pasa a COMPLETADA.

  @Test
  @DisplayName("CP-22i Una reserva que no está CONFIRMADA no se puede completar (409)")
  void completarSinConfirmarFalla() throws Exception {
    List<Reserva> noConfirmadas =
        List.of(
            reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.PENDIENTE_PAGO),
            reserva(equipoDos, LAS_TRES_PM, 1, EstadoReserva.EXPIRADA),
            reserva(equipoUno, MEDIANOCHE, 1, EstadoReserva.CANCELADA));

    for (Reserva reserva : noConfirmadas) {
      EstadoReserva estadoAntes = reserva.getEstado();
      mockMvc
          .perform(
              post(RUTA + "/" + reserva.getId() + "/completar")
                  .header("Authorization", tokenAdmin))
          .andExpect(status().isConflict());

      entityManager.refresh(reserva);
      assertEquals(estadoAntes, reserva.getEstado());
    }
    entityManager.refresh(cliente);
    assertEquals(0, cliente.getPuntosFidelidad());
  }

  // Alcance: casos para resolver a mano y reservas inexistentes.

  @Test
  @DisplayName("CP-22j El admin ve las reservas que expiraron con un pago aprobado")
  void listaExpiradasConPagoAprobado() throws Exception {
    Reserva expiradaConPago = reserva(equipoUno, LAS_TRES_PM, 1, EstadoReserva.EXPIRADA);
    pagoRepository.saveAndFlush(
        Pago.builder()
            .reserva(expiradaConPago)
            .referencia("REF-PRUEBA-EXPIRADA-PAGADA")
            .montoCentavos(250_000L)
            .estado(EstadoPago.APROBADO)
            .build());
    reserva(equipoDos, LAS_TRES_PM, 1, EstadoReserva.EXPIRADA); // expirada sin pago

    mockMvc
        .perform(get(RUTA + "/expiradas-con-pago").header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].reserva.id").value(expiradaConPago.getId()))
        .andExpect(jsonPath("$[0].reserva.estado").value("EXPIRADA"))
        .andExpect(jsonPath("$[0].referenciaPago").value("REF-PRUEBA-EXPIRADA-PAGADA"))
        .andExpect(jsonPath("$[0].montoPagadoCentavos").value(250_000));
  }

  @Test
  @DisplayName("CP-22k Cancelar o completar una reserva que no existe responde 404")
  void reservaInexistenteRecibe404() throws Exception {
    mockMvc
        .perform(post(RUTA + "/999999/completar").header("Authorization", tokenAdmin))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post(RUTA + "/999999/cancelar")
                .header("Authorization", tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConMotivo("No existe")))
        .andExpect(status().isNotFound());
  }
}
