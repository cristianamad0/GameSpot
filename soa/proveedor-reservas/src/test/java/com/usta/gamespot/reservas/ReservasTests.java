package com.usta.gamespot.reservas;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del proveedor de reservas (CACS-26) con la base H2 en memoria y los usuarios de prueba
 * de data.sql. Usan fechas fijas en el futuro (2099) y cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservasTests {

  /** 2 de marzo de 2099, 15:00 en Bogotá (UTC-5). */
  private static final String TRES_PM = "2099-03-02T20:00:00Z";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManager entityManager;

  /** Simula que la reserva se creó hace {@code minutos} minutos (CACS-47). */
  private void crearHace(long id, int minutos) {
    entityManager.flush();
    jdbc.update(
        "update reservas set creada_en = dateadd('MINUTE', ?, creada_en) where id = ?", -minutos, id);
    entityManager.clear();
  }

  private String token(String email, String password) throws Exception {
    String respuesta =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(respuesta, "$.token");
  }

  private String laura() throws Exception {
    return token("laura@gamespot.demo", "ClienteDemo123");
  }

  private String andres() throws Exception {
    return token("andres@gamespot.demo", "ClienteDemo123");
  }

  private static String reserva(long equipoId, String inicio, int duracionMinutos) {
    return "{\"equipoId\":"
        + equipoId
        + ",\"nombreEquipo\":\"Xbox Series S #2\",\"tarifaHoraPesos\":5000,"
        + "\"tarifaMediaHoraPesos\":3000,\"inicio\":\""
        + inicio
        + "\",\"duracionMinutos\":"
        + duracionMinutos
        + "}";
  }

  private ResultActions crear(String token, String cuerpo) throws Exception {
    var peticion = post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    if (token != null) {
      peticion.header("Authorization", token);
    }
    return mockMvc.perform(peticion);
  }

  private long crearYObtenerId(String token, String cuerpo) throws Exception {
    String respuesta =
        crear(token, cuerpo).andExpect(status().isCreated()).andReturn().getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
  }

  @Test
  @DisplayName("CP-37a El login devuelve un JWT con el rol; credenciales incorrectas dan 401")
  void loginDevuelveToken() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@gamespot.demo\",\"password\":\"AdminDemo123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("Bearer"))
        .andExpect(jsonPath("$.rol").value("ADMIN"))
        .andExpect(jsonPath("$.token").isNotEmpty());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"laura@gamespot.demo\",\"password\":\"otra\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
  }

  @Test
  @DisplayName("CP-37b Crea la reserva en PENDIENTE_PAGO con precio y adelanto del 50 %")
  void creaLaReserva() throws Exception {
    crear(laura(), reserva(3, TRES_PM, 90))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
        .andExpect(jsonPath("$.precioTotalPesos").value(8000))
        .andExpect(jsonPath("$.adelantoPesos").value(4000))
        .andExpect(jsonPath("$.fin").value("2099-03-02T21:30:00Z"))
        .andExpect(jsonPath("$.emailCliente").value("laura@gamespot.demo"));
  }

  @Test
  @DisplayName("CP-37c Bloques mal alineados, duraciones inválidas o en el pasado dan 400")
  void validaLosBloques() throws Exception {
    String token = laura();
    crear(token, reserva(3, "2099-03-02T20:15:00Z", 60))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("La reserva debe empezar a las :00 o a las :30"));
    crear(token, reserva(3, TRES_PM, 45))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("La duración debe ser múltiplo de 30 minutos"));
    crear(token, reserva(3, "2020-01-01T20:00:00Z", 60))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("No se puede reservar un bloque que ya empezó"));
    crear(token, "{\"equipoId\":3,\"duracionMinutos\":300}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Datos inválidos"))
        .andExpect(jsonPath("$.detalles", hasSize(5)));
  }

  @Test
  @DisplayName("CP-37d Un bloque que se cruza con otra reserva activa del mismo equipo da 409")
  void rechazaLosCruces() throws Exception {
    crear(laura(), reserva(3, TRES_PM, 60)).andExpect(status().isCreated());
    crear(andres(), reserva(3, "2099-03-02T20:30:00Z", 60))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("El bloque ya está reservado para ese equipo"));
    crear(andres(), reserva(4, "2099-03-02T20:30:00Z", 60)).andExpect(status().isCreated());
    crear(andres(), reserva(3, "2099-03-02T21:00:00Z", 60)).andExpect(status().isCreated());
  }

  @Test
  @DisplayName("CP-37e Cada cliente ve solo sus reservas y la ocupación es pública y sin datos del cliente")
  void misReservasYOcupacion() throws Exception {
    crear(laura(), reserva(3, TRES_PM, 60)).andExpect(status().isCreated());
    crear(andres(), reserva(3, "2099-03-02T22:00:00Z", 30)).andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/reservas/mias").header("Authorization", laura()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].emailCliente").value("laura@gamespot.demo"));
    mockMvc
        .perform(get("/api/reservas/ocupacion").param("equipoId", "3").param("fecha", "2099-03-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].inicio").value(TRES_PM))
        .andExpect(jsonPath("$[0].emailCliente").doesNotExist());
  }

  @Test
  @DisplayName("CP-37f Pagar confirma la reserva; pagarla otra vez da 409")
  void pagarConfirma() throws Exception {
    String token = laura();
    long id = crearYObtenerId(token, reserva(3, TRES_PM, 60));
    mockMvc
        .perform(patch("/api/reservas/" + id + "/pago").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    mockMvc
        .perform(patch("/api/reservas/" + id + "/pago").header("Authorization", token))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("CP-37g Cancelar es lógico y libera el bloque; otro cliente recibe 403")
  void cancelarEsLogico() throws Exception {
    String token = laura();
    long id = crearYObtenerId(token, reserva(3, TRES_PM, 60));
    mockMvc
        .perform(patch("/api/reservas/" + id + "/cancelacion").header("Authorization", andres()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(patch("/api/reservas/" + id + "/cancelacion").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADA"));
    mockMvc
        .perform(get("/api/reservas/" + id).header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADA"));
    crear(andres(), reserva(3, TRES_PM, 60)).andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
      "CP-37i El precio cobra cada hora a la tarifa por hora y la media hora que sobra a su propia"
          + " tarifa")
  void precioConMediaHora() throws Exception {
    String token = laura();
    crear(token, reserva(3, TRES_PM, 30))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precioTotalPesos").value(3000))
        .andExpect(jsonPath("$.adelantoPesos").value(1500));
    crear(token, reserva(4, TRES_PM, 60))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precioTotalPesos").value(5000))
        .andExpect(jsonPath("$.adelantoPesos").value(2500));
    crear(token, reserva(5, TRES_PM, 150))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precioTotalPesos").value(13000))
        .andExpect(jsonPath("$.adelantoPesos").value(6500));
  }

  @Test
  @DisplayName("CP-37h Sin token o con token inválido las operaciones protegidas dan 401")
  void exigeToken() throws Exception {
    crear(null, reserva(3, TRES_PM, 60))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
    crear("Bearer token-inventado", reserva(3, TRES_PM, 60)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/reservas/mias")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/reservas/9999").header("Authorization", laura()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "CP-37j Una reserva sin pagar vence a los 15 minutos: queda EXPIRADA, libera el bloque y"
          + " pagarla o cancelarla da 409")
  void reservaSinPagarVence() throws Exception {
    String token = laura();
    long id = crearYObtenerId(token, reserva(3, TRES_PM, 60));
    crearHace(id, 16);

    mockMvc
        .perform(get("/api/reservas/mias").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].estado").value("EXPIRADA"))
        .andExpect(jsonPath("$[0].pagarAntesDe").doesNotExist());
    mockMvc
        .perform(get("/api/reservas/ocupacion").param("equipoId", "3").param("fecha", "2099-03-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(patch("/api/reservas/" + id + "/pago").header("Authorization", token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje", containsString("venció")));
    mockMvc
        .perform(patch("/api/reservas/" + id + "/cancelacion").header("Authorization", token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje", containsString("ya venció")));
    crear(andres(), reserva(3, TRES_PM, 60)).andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
      "CP-37k Dentro de los 15 minutos la reserva sigue pendiente, informa su límite de pago y"
          + " bloquea el turno; pagada ya no vence")
  void reservaDentroDelPlazo() throws Exception {
    String token = laura();
    long id = crearYObtenerId(token, reserva(3, TRES_PM, 60));
    mockMvc
        .perform(get("/api/reservas/" + id).header("Authorization", token))
        .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
        .andExpect(jsonPath("$.pagarAntesDe").exists());
    crearHace(id, 14);
    crear(andres(), reserva(3, TRES_PM, 60)).andExpect(status().isConflict());

    mockMvc
        .perform(patch("/api/reservas/" + id + "/pago").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    crearHace(id, 60);
    mockMvc
        .perform(get("/api/reservas/" + id).header("Authorization", token))
        .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    crear(andres(), reserva(3, TRES_PM, 60)).andExpect(status().isConflict());
  }
}
