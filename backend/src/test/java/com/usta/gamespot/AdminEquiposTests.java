package com.usta.gamespot;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Pruebas de la gestión de equipos y tarifas desde el panel (HU-06, CACS-22). Llaman a los
 * endpoints con tokens JWT reales de un ADMIN y de un CLIENTE. Usan fechas de 2099 para que las
 * reservas siempre estén en el futuro. Cada prueba corre en una transacción que se revierte al
 * terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class AdminEquiposTests {

  /** Lunes 2 de marzo de 2099, 15:00 en Bogotá (UTC-5). Se atiende de 14:00 a 22:00. */
  private static final Instant LUNES_TRES_PM = Instant.parse("2099-03-02T20:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private EntityManager entityManager;

  private Usuario cliente;
  private String tokenAdmin;
  private String tokenCliente;
  private Equipo xbox360;

  @BeforeEach
  void prepararDatos() {
    tokenAdmin = "Bearer " + tokenDe(crearUsuario("admin@gamespot.test", Rol.ADMIN));
    cliente = crearUsuario("cliente@gamespot.test", Rol.CLIENTE);
    tokenCliente = "Bearer " + tokenDe(cliente);
    xbox360 =
        equipoRepository.findAll().stream()
            .filter(e -> e.getNombre().equals("Xbox 360 #1"))
            .findFirst()
            .orElseThrow();
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

  private ResultActions cambiarDisponibilidad(Long equipoId, String cuerpo, String token)
      throws Exception {
    return mockMvc.perform(
        put("/api/admin/equipos/" + equipoId + "/disponibilidad")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }

  private ResultActions actualizarModalidad(Long modalidadId, String cuerpo, String token)
      throws Exception {
    return mockMvc.perform(
        put("/api/admin/modalidades/" + modalidadId)
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }

  private ResultActions reservarComoCliente(Long equipoId, Instant inicio) throws Exception {
    return mockMvc.perform(
        post("/api/reservas")
            .header("Authorization", tokenCliente)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"equipoId\": %d, \"inicio\": \"%s\", \"duracionMinutos\": 60}"
                    .formatted(equipoId, inicio)));
  }

  /** Reserva confirmada de una hora del Xbox 360, guardada directamente en la base. */
  private Reserva reservaConfirmada(Instant inicio) {
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(xbox360)
            .inicio(inicio)
            .fin(inicio.plus(1, ChronoUnit.HOURS))
            .precioTotalPesos(5000)
            .adelantoPesos(2500)
            .estado(EstadoReserva.CONFIRMADA)
            .tieneAnticipoPagado(true)
            .build());
  }

  // Criterio 1: el administrador ve todos los equipos con modalidad, tarifa y estado.

  @Test
  @DisplayName("CP-33a El admin ve los 6 equipos con modalidad, tarifa y estado, y las modalidades")
  void elAdminVeEquiposYModalidades() throws Exception {
    mockMvc
        .perform(get("/api/admin/equipos").header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(6)))
        .andExpect(jsonPath("$[0].nombre").value("Xbox 360 #1"))
        .andExpect(jsonPath("$[0].modalidad").value("Xbox 360"))
        .andExpect(jsonPath("$[0].tarifaHoraPesos").value(5000))
        .andExpect(jsonPath("$[*].estaDisponible", everyItem(is(true))))
        .andExpect(jsonPath("$[0].reservasActivasFuturas").value(0));

    mockMvc
        .perform(get("/api/admin/modalidades").header("Authorization", tokenAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[1].nombre").value("Xbox Series S"))
        .andExpect(jsonPath("$[1].tarifaHoraPesos").value(6000))
        .andExpect(jsonPath("$[1].cantidadEquipos").value(4));
  }

  // Criterio 2: fuera de servicio, sin bloques libres y 409 al reservar.

  @Test
  @DisplayName("CP-33b Fuera de servicio no deja bloques libres y reservar responde 409")
  void fueraDeServicioNoSeReserva() throws Exception {
    cambiarDisponibilidad(xbox360.getId(), "{\"estaDisponible\": false}", tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estaDisponible").value(false));

    mockMvc
        .perform(
            get("/api/reservas/disponibilidad")
                .header("Authorization", tokenCliente)
                .param("equipoId", xbox360.getId().toString())
                .param("fecha", "2099-03-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[*].estaLibre", everyItem(is(false))));
    reservarComoCliente(xbox360.getId(), LUNES_TRES_PM).andExpect(status().isConflict());

    // Al volver a habilitarlo se puede reservar de nuevo.
    cambiarDisponibilidad(xbox360.getId(), "{\"estaDisponible\": true}", tokenAdmin)
        .andExpect(status().isOk());
    reservarComoCliente(xbox360.getId(), LUNES_TRES_PM).andExpect(status().isCreated());
  }

  // Criterio 3: las reservas existentes no cambian y el panel advierte cuántas hay.

  @Test
  @DisplayName("CP-33c Sus reservas futuras siguen igual y el panel advierte cuántas son")
  void reservasExistentesNoCambian() throws Exception {
    Reserva futura = reservaConfirmada(LUNES_TRES_PM);
    reservaConfirmada(LUNES_TRES_PM.plus(2, ChronoUnit.HOURS));
    // Una reserva pasada no cuenta para la advertencia.
    reservaConfirmada(Instant.parse("2026-10-10T20:00:00Z"));

    cambiarDisponibilidad(xbox360.getId(), "{\"estaDisponible\": false}", tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservasActivasFuturas").value(2));

    entityManager.refresh(futura);
    assertEquals(EstadoReserva.CONFIRMADA, futura.getEstado());
    assertEquals(5000, futura.getPrecioTotalPesos());
  }

  // Criterio 4: la tarifa nueva aplica a las reservas nuevas; las existentes conservan su precio.

  @Test
  @DisplayName("CP-33d Al cambiar la tarifa, las reservas nuevas usan el precio nuevo")
  void tarifaNuevaSoloParaReservasNuevas() throws Exception {
    Reserva existente = reservaConfirmada(LUNES_TRES_PM.plus(3, ChronoUnit.HOURS));
    Long modalidadId = xbox360.getModalidad().getId();

    actualizarModalidad(
            modalidadId, "{\"tarifaHoraPesos\": 8000, \"estaActiva\": true}", tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tarifaHoraPesos").value(8000))
        .andExpect(jsonPath("$.cantidadEquipos").value(1));

    reservarComoCliente(xbox360.getId(), LUNES_TRES_PM)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precioTotalPesos").value(8000))
        .andExpect(jsonPath("$.adelantoPesos").value(4000));
    mockMvc
        .perform(get("/api/catalogo").header("Authorization", tokenCliente))
        // El catálogo va del más económico al más costoso: ahora el Xbox 360 queda de último.
        .andExpect(jsonPath("$.equipos[5].nombre").value("Xbox 360 #1"))
        .andExpect(jsonPath("$.equipos[5].tarifaHoraPesos").value(8000));

    entityManager.refresh(existente);
    assertEquals(5000, existente.getPrecioTotalPesos());
  }

  @Test
  @DisplayName("CP-33e Una modalidad desactivada deja sus equipos sin reservar")
  void modalidadDesactivada() throws Exception {
    actualizarModalidad(
            xbox360.getModalidad().getId(),
            "{\"tarifaHoraPesos\": 5000, \"estaActiva\": false}",
            tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estaActiva").value(false));

    mockMvc
        .perform(get("/api/admin/equipos").header("Authorization", tokenAdmin))
        .andExpect(jsonPath("$[0].modalidadEstaActiva").value(false));
    reservarComoCliente(xbox360.getId(), LUNES_TRES_PM).andExpect(status().isConflict());
  }

  // Criterio 5: 400 y 404 con mensaje claro.

  @Test
  @DisplayName("CP-33f Una tarifa no positiva responde 400 y un id inexistente responde 404")
  void validacionesYNoEncontrados() throws Exception {
    Long modalidadId = xbox360.getModalidad().getId();
    actualizarModalidad(modalidadId, "{\"tarifaHoraPesos\": 0, \"estaActiva\": true}", tokenAdmin)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detalles[0]").value("tarifaHoraPesos: debe ser mayor que 0"));
    actualizarModalidad(
            modalidadId, "{\"tarifaHoraPesos\": -100, \"estaActiva\": true}", tokenAdmin)
        .andExpect(status().isBadRequest());
    cambiarDisponibilidad(xbox360.getId(), "{}", tokenAdmin).andExpect(status().isBadRequest());

    cambiarDisponibilidad(999_999L, "{\"estaDisponible\": false}", tokenAdmin)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No existe el equipo con id 999999"));
    actualizarModalidad(999_999L, "{\"tarifaHoraPesos\": 5000, \"estaActiva\": true}", tokenAdmin)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No existe la modalidad con id 999999"));
  }

  @Test
  @DisplayName("CP-33h Una tarifa mayor que 1.000.000 responde 400 y no cambia la tarifa")
  void tarifaConTope() throws Exception {
    Long modalidadId = xbox360.getModalidad().getId();
    actualizarModalidad(
            modalidadId, "{\"tarifaHoraPesos\": 1000001, \"estaActiva\": true}", tokenAdmin)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detalles[0]").value("tarifaHoraPesos: no puede superar 1.000.000"));

    // El tope exacto sí se acepta.
    actualizarModalidad(
            modalidadId, "{\"tarifaHoraPesos\": 1000000, \"estaActiva\": true}", tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tarifaHoraPesos").value(1_000_000));
  }

  // Criterio 6: un CLIENTE recibe 403.

  @Test
  @DisplayName("CP-33g Un CLIENTE recibe 403 en todos los endpoints de equipos y tarifas")
  void clienteRecibe403() throws Exception {
    mockMvc
        .perform(get("/api/admin/equipos").header("Authorization", tokenCliente))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/admin/modalidades").header("Authorization", tokenCliente))
        .andExpect(status().isForbidden());
    cambiarDisponibilidad(xbox360.getId(), "{\"estaDisponible\": false}", tokenCliente)
        .andExpect(status().isForbidden());
    actualizarModalidad(
            xbox360.getModalidad().getId(),
            "{\"tarifaHoraPesos\": 1, \"estaActiva\": true}",
            tokenCliente)
        .andExpect(status().isForbidden());

    entityManager.refresh(xbox360);
    assertEquals(true, xbox360.isEstaDisponible());
    assertEquals(5000, xbox360.getModalidad().getTarifaHoraPesos());
  }
}
