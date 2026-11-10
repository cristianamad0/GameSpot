package com.usta.gamespot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del historial de reservas del cliente (HU-07, CACS-3). Llaman al endpoint con un token
 * JWT real; las reservas se crean directamente en la base. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class HistorialReservasTests {

  /** 1 de octubre de 2026, 15:00 en Bogotá (UTC-5). Cada reserva va un día después. */
  private static final Instant PRIMER_DIA = Instant.parse("2026-10-01T20:00:00Z");

  private static final String HISTORIAL = "/api/reservas/mias";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;

  private Usuario laura;
  private Usuario andres;
  private Equipo equipo;

  @BeforeEach
  void prepararDatos() {
    laura = crearUsuario("laura.historial@gamespot.test");
    andres = crearUsuario("andres.historial@gamespot.test");
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

  private String token(Usuario usuario) {
    return "Bearer "
        + jwtService.generarToken(usuarioDetailsService.loadUserByUsername(usuario.getEmail()));
  }

  /** Reserva de una hora del usuario, el día número {@code dia} desde el 1 de octubre. */
  private Reserva reserva(Usuario usuario, int dia, EstadoReserva estado) {
    Instant inicio = PRIMER_DIA.plus(dia, ChronoUnit.DAYS);
    boolean estaPagada = estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA;
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(usuario)
            .equipo(equipo)
            .inicio(inicio)
            .fin(inicio.plus(1, ChronoUnit.HOURS))
            .precioTotalPesos(5000)
            .adelantoPesos(2500)
            .estado(estado)
            .tieneAnticipoPagado(estaPagada)
            .expiraEn(estado == EstadoReserva.PENDIENTE_PAGO ? inicio : null)
            .canceladaEn(estado == EstadoReserva.CANCELADA ? inicio : null)
            .build());
  }

  private ResultActions historial(Usuario usuario, String consulta) throws Exception {
    return mockMvc.perform(get(HISTORIAL + consulta).header("Authorization", token(usuario)));
  }

  // Criterio 1: solo las reservas propias.

  @Test
  @DisplayName("CP-34a El cliente ve solo sus reservas, nunca las de otro usuario")
  void soloReservasPropias() throws Exception {
    Reserva deLaura = reserva(laura, 0, EstadoReserva.CONFIRMADA);
    reserva(andres, 1, EstadoReserva.CONFIRMADA);
    reserva(andres, 2, EstadoReserva.PENDIENTE_PAGO);

    historial(laura, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReservas").value(1))
        .andExpect(jsonPath("$.reservas.length()").value(1))
        .andExpect(jsonPath("$.reservas[0].id").value(deLaura.getId()))
        .andExpect(jsonPath("$.reservas[0].nombreEquipo").value(equipo.getNombre()))
        .andExpect(jsonPath("$.reservas[0].precioTotalPesos").value(5000))
        .andExpect(jsonPath("$.reservas[0].adelantoPesos").value(2500))
        .andExpect(jsonPath("$.reservas[0].estado").value("CONFIRMADA"));
  }

  // Criterio 2: de la más reciente a la más antigua.

  @Test
  @DisplayName("CP-34b Las reservas van de la más reciente a la más antigua")
  void ordenDeLaMasRecienteALaMasAntigua() throws Exception {
    Reserva antigua = reserva(laura, 0, EstadoReserva.COMPLETADA);
    Reserva reciente = reserva(laura, 5, EstadoReserva.PENDIENTE_PAGO);
    Reserva intermedia = reserva(laura, 2, EstadoReserva.CANCELADA);

    historial(laura, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservas[0].id").value(reciente.getId()))
        .andExpect(jsonPath("$.reservas[1].id").value(intermedia.getId()))
        .andExpect(jsonPath("$.reservas[2].id").value(antigua.getId()));
  }

  // Criterio 3: filtro por estado.

  @Test
  @DisplayName("CP-34c Se filtra por estado: solo CONFIRMADA")
  void filtroPorEstado() throws Exception {
    reserva(laura, 0, EstadoReserva.COMPLETADA);
    Reserva confirmada = reserva(laura, 1, EstadoReserva.CONFIRMADA);
    reserva(laura, 2, EstadoReserva.CANCELADA);
    reserva(laura, 3, EstadoReserva.EXPIRADA);

    historial(laura, "?estado=CONFIRMADA")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReservas").value(1))
        .andExpect(jsonPath("$.reservas[0].id").value(confirmada.getId()));
  }

  // Criterio 4: paginado, 10 por página por defecto.

  @Test
  @DisplayName("CP-34d El listado trae 10 por página por defecto y se pide la página siguiente")
  void paginadoDeDiezPorDefecto() throws Exception {
    for (int dia = 0; dia < 12; dia++) {
      reserva(laura, dia, EstadoReserva.COMPLETADA);
    }

    historial(laura, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservas.length()").value(10))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamanoPagina").value(10))
        .andExpect(jsonPath("$.totalReservas").value(12))
        .andExpect(jsonPath("$.totalPaginas").value(2));
    historial(laura, "?pagina=1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservas.length()").value(2))
        .andExpect(jsonPath("$.pagina").value(1));
    historial(laura, "?tamano=5&pagina=2")
        .andExpect(jsonPath("$.reservas.length()").value(2))
        .andExpect(jsonPath("$.totalPaginas").value(3));
  }

  // Criterio 5: sin reservas, lista vacía.

  @Test
  @DisplayName("CP-34e Un cliente sin reservas recibe una lista vacía, no un error")
  void sinReservasListaVacia() throws Exception {
    historial(laura, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservas.length()").value(0))
        .andExpect(jsonPath("$.totalReservas").value(0))
        .andExpect(jsonPath("$.totalPaginas").value(0));
  }

  @Test
  @DisplayName("CP-34f Página, tamaño o estado inválidos responden 400 y sin token 401")
  void parametrosInvalidos() throws Exception {
    historial(laura, "?pagina=-1").andExpect(status().isBadRequest());
    historial(laura, "?tamano=0").andExpect(status().isBadRequest());
    historial(laura, "?tamano=51").andExpect(status().isBadRequest());
    historial(laura, "?estado=JUGANDO").andExpect(status().isBadRequest());
    mockMvc.perform(get(HISTORIAL)).andExpect(status().isUnauthorized());
  }
}
