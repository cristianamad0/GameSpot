package com.usta.gamespot.portal;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.portal.cliente.CatalogoCliente;
import com.usta.gamespot.portal.cliente.ReservasCliente;
import com.usta.gamespot.portal.dto.BloqueOcupadoDto;
import com.usta.gamespot.portal.dto.CrearReservaDto;
import com.usta.gamespot.portal.dto.EquipoDto;
import com.usta.gamespot.portal.dto.HorarioDto;
import com.usta.gamespot.portal.dto.ReservaDto;
import com.usta.gamespot.portal.exception.ServicioNoDisponibleException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de la consumidora (CACS-27) con los dos proveedores simulados: se verifica que el backend
 * coordine bien a ambos y traduzca sus fallos. Las llamadas HTTP reales de los clientes se prueban
 * en ClientesHttpTests. El reloj está fijo el lunes 2 de marzo de 2099 a las 3:10 p. m. en Bogotá.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PortalTests {

  private static final String TOKEN = "Bearer token-de-prueba";
  private static final LocalDate LUNES = LocalDate.parse("2099-03-02");

  @TestConfiguration
  static class RelojFijo {
    @Bean
    @Primary
    Clock relojFijo() {
      return Clock.fixed(Instant.parse("2099-03-02T20:10:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CatalogoCliente catalogoCliente;
  @MockitoBean private ReservasCliente reservasCliente;

  private final EquipoDto seriesS =
      new EquipoDto(3L, "Xbox Series S #2", 2L, "Xbox Series S", 6000, true, true);
  private final EquipoDto fueraDeServicio =
      new EquipoDto(6L, "Sala VIP Xbox Series X", 3L, "Sala VIP Xbox Series X", 7000, false, false);

  @BeforeEach
  void catalogoBase() {
    when(catalogoCliente.obtenerEquipo(3L)).thenReturn(seriesS);
    when(catalogoCliente.obtenerEquipo(6L)).thenReturn(fueraDeServicio);
    when(catalogoCliente.listarHorarios())
        .thenReturn(List.of(new HorarioDto((short) 1, LocalTime.of(14, 0), LocalTime.of(22, 0))));
  }

  private String reserva(long equipoId, String hora, int duracionMinutos) {
    return "{\"equipoId\":"
        + equipoId
        + ",\"fecha\":\"2099-03-02\",\"hora\":\""
        + hora
        + "\",\"duracionMinutos\":"
        + duracionMinutos
        + "}";
  }

  @Test
  @DisplayName("CP-38a Lista los equipos del proveedor de catálogo")
  void listaEquipos() throws Exception {
    when(catalogoCliente.listarEquipos()).thenReturn(List.of(seriesS, fueraDeServicio));
    mockMvc
        .perform(get("/api/portal/equipos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].nombre").value("Xbox Series S #2"));
  }

  @Test
  @DisplayName("CP-38b La disponibilidad cruza el horario del catálogo con la ocupación de reservas")
  void disponibilidadIntegrada() throws Exception {
    when(reservasCliente.ocupacion(3L, LUNES))
        .thenReturn(
            List.of(
                new BloqueOcupadoDto(
                    Instant.parse("2099-03-02T21:00:00Z"), Instant.parse("2099-03-02T22:00:00Z"))));
    mockMvc
        .perform(get("/api/portal/equipos/3/disponibilidad").param("fecha", "2099-03-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.atiende").value(true))
        .andExpect(jsonPath("$.bloques", hasSize(16)))
        .andExpect(jsonPath("$.bloques[2].motivo").value("PASADO"))
        .andExpect(jsonPath("$.bloques[3].estaLibre").value(true))
        .andExpect(jsonPath("$.bloques[4].motivo").value("OCUPADO"))
        .andExpect(jsonPath("$.bloques[5].motivo").value("OCUPADO"))
        .andExpect(jsonPath("$.bloques[6].estaLibre").value(true));
  }

  @Test
  @DisplayName("CP-38c Reservar toma nombre y tarifa del catálogo y crea la reserva en el proveedor de reservas")
  void reservarIntegrado() throws Exception {
    CrearReservaDto esperado =
        new CrearReservaDto(3L, "Xbox Series S #2", 6000, Instant.parse("2099-03-02T21:00:00Z"), 60);
    when(reservasCliente.crear(eq(esperado), eq(TOKEN)))
        .thenReturn(
            new ReservaDto(
                10L,
                3L,
                "Xbox Series S #2",
                esperado.inicio(),
                Instant.parse("2099-03-02T22:00:00Z"),
                6000,
                3000,
                "PENDIENTE_PAGO",
                "laura@gamespot.demo"));
    mockMvc
        .perform(
            post("/api/portal/reservas")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(3, "16:00", 60)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
        .andExpect(jsonPath("$.adelantoPesos").value(3000));
    verify(reservasCliente).crear(esperado, TOKEN);
  }

  @Test
  @DisplayName("CP-38d Fuera del horario da 400, equipo fuera de servicio 409, datos inválidos 400 y sin sesión 401")
  void validacionesDeLaConsumidora() throws Exception {
    mockMvc
        .perform(
            post("/api/portal/reservas")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(3, "21:30", 60)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value("La reserva debe estar dentro del horario de ese día: de 14:00 a 22:00"));
    mockMvc
        .perform(
            post("/api/portal/reservas")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(6, "16:00", 60)))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            post("/api/portal/reservas")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipoId\":3,\"duracionMinutos\":10}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detalles", hasSize(3)));
    mockMvc
        .perform(
            post("/api/portal/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reserva(3, "16:00", 60)))
        .andExpect(status().isUnauthorized());
    verify(reservasCliente, never()).crear(any(), any());
  }

  @Test
  @DisplayName("CP-38e Con el catálogo detenido la consumidora responde 503 y sigue atendiendo lo demás")
  void proveedorDetenido() throws Exception {
    when(catalogoCliente.listarEquipos())
        .thenThrow(new ServicioNoDisponibleException("catálogo", new RuntimeException()));
    when(catalogoCliente.estaDisponible()).thenReturn(false);
    when(reservasCliente.estaDisponible()).thenReturn(true);
    when(reservasCliente.mias(TOKEN)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/portal/equipos"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "El servicio de catálogo no está disponible en este momento. Intenta de nuevo en"
                        + " unos minutos."));
    mockMvc
        .perform(get("/api/portal/estado"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.catalogo").value("NO_DISPONIBLE"))
        .andExpect(jsonPath("$.reservas").value("DISPONIBLE"));
    mockMvc
        .perform(get("/api/portal/reservas/mias").header("Authorization", TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("CP-38f La página web se sirve desde la consumidora")
  void sirveLaPagina() throws Exception {
    mockMvc
        .perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("app.js")));
  }
}
