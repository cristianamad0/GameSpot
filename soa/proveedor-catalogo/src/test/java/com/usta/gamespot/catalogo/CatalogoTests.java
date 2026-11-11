package com.usta.gamespot.catalogo;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.catalogo.repository.EquipoRepository;
import com.usta.gamespot.catalogo.repository.ModalidadRepository;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityManager;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del proveedor de catálogo (CACS-25) con la base H2 en memoria y los datos de data.sql.
 * Los tokens se firman aquí con el secreto de pruebas, como los emitiría el proveedor de
 * reservas. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ModalidadRepository modalidadRepository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;
  @Autowired private EntityManager entityManager;

  @Value("${app.jwt.secret}")
  private String secreto;

  private String token(String rol) {
    return "Bearer "
        + Jwts.builder()
            .setSubject(rol.toLowerCase() + "@gamespot.test")
            .claim("rol", rol)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
            .compact();
  }

  private Long modalidad(String nombre) {
    return modalidadRepository.findAll().stream()
        .filter(m -> m.getNombre().equals(nombre))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private Long equipo(String nombre) {
    return equipoRepository.findAll().stream()
        .filter(e -> e.getNombre().equals(nombre))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private ResultActions enviar(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
      String token,
      String cuerpo)
      throws Exception {
    if (token != null) {
      peticion.header("Authorization", token);
    }
    if (cuerpo != null) {
      peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }
    return mockMvc.perform(peticion);
  }

  @Test
  @DisplayName(
      "CP-36a Lista los 5 equipos reales con modalidad, tarifas por hora y media hora y estado"
          + " (DML de data.sql)")
  void listaLosEquipos() throws Exception {
    mockMvc
        .perform(get("/api/equipos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)))
        .andExpect(jsonPath("$[0].nombre").value("Xbox 360 #1"))
        .andExpect(jsonPath("$[1].nombre").value("Xbox 360 #2"))
        .andExpect(jsonPath("$[0].tarifaHoraPesos").value(4000))
        .andExpect(jsonPath("$[0].tarifaMediaHoraPesos").value(2500))
        .andExpect(jsonPath("$[2].tarifaHoraPesos").value(5000))
        .andExpect(jsonPath("$[2].tarifaMediaHoraPesos").value(3000))
        .andExpect(jsonPath("$[4].nombre").value("Sala VIP Xbox Series X"))
        .andExpect(jsonPath("$[4].tarifaHoraPesos").value(10000))
        .andExpect(jsonPath("$[4].tarifaMediaHoraPesos").value(6000))
        .andExpect(jsonPath("$[0].sePuedeReservar").value(true));
  }

  @Test
  @DisplayName("CP-36b Consulta un equipo por id y responde 404 si no existe")
  void consultaUnEquipo() throws Exception {
    mockMvc
        .perform(get("/api/equipos/" + equipo("Xbox Series S #2")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modalidad").value("Xbox Series S"));
    mockMvc
        .perform(get("/api/equipos/9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No existe el equipo con id 9999"));
  }

  @Test
  @DisplayName("CP-36c CRUD completo de equipos con token de administrador")
  void crudDeEquipos() throws Exception {
    String admin = token("ADMIN");
    String cuerpo =
        "{\"nombre\":\"Xbox Series S #5\",\"modalidadId\":"
            + modalidad("Xbox Series S")
            + ",\"estaDisponible\":true}";
    enviar(post("/api/equipos"), admin, cuerpo)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("Xbox Series S #5"));
    Long id = equipo("Xbox Series S #5");

    String fueraDeServicio = cuerpo.replace("\"estaDisponible\":true", "\"estaDisponible\":false");
    enviar(put("/api/equipos/" + id), admin, fueraDeServicio)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estaDisponible").value(false))
        .andExpect(jsonPath("$.sePuedeReservar").value(false));

    enviar(delete("/api/equipos/" + id), admin, null).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/equipos/" + id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("CP-36d Valida los datos (400) y no permite nombres repetidos (409)")
  void validaLosDatos() throws Exception {
    String admin = token("ADMIN");
    enviar(post("/api/equipos"), admin, "{\"nombre\":\"\",\"estaDisponible\":true}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Datos inválidos"))
        .andExpect(jsonPath("$.detalles", hasSize(3)));
    enviar(post("/api/equipos"), admin, "{no es json")
        .andExpect(status().isBadRequest());
    String repetido =
        "{\"nombre\":\"xbox 360 #1\",\"modalidadId\":"
            + modalidad("Xbox 360")
            + ",\"estaDisponible\":true}";
    enviar(post("/api/equipos"), admin, repetido).andExpect(status().isConflict());
    enviar(
            put("/api/modalidades/" + modalidad("Xbox 360")),
            admin,
            "{\"tarifaHoraPesos\":0,\"estaActiva\":true}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detalles", hasSize(2)));
  }

  @Test
  @DisplayName("CP-36e Sin token o con token inválido responde 401; un CLIENTE recibe 403")
  void exigeTokenDeAdministrador() throws Exception {
    Long id = equipo("Xbox 360 #1");
    enviar(delete("/api/equipos/" + id), null, null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
    enviar(delete("/api/equipos/" + id), "Bearer token-inventado", null)
        .andExpect(status().isUnauthorized());
    enviar(delete("/api/equipos/" + id), token("CLIENTE"), null)
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/equipos/" + id)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("CP-36f Cambia la tarifa de una modalidad y la lista de equipos la refleja")
  void cambiaLaTarifa() throws Exception {
    Long id = modalidad("Xbox 360");
    enviar(
            put("/api/modalidades/" + id),
            token("ADMIN"),
            "{\"tarifaHoraPesos\":4500,\"tarifaMediaHoraPesos\":2800,\"estaActiva\":true}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tarifaHoraPesos").value(4500))
        .andExpect(jsonPath("$.tarifaMediaHoraPesos").value(2800));
    mockMvc
        .perform(get("/api/equipos/" + equipo("Xbox 360 #1")))
        .andExpect(jsonPath("$.tarifaHoraPesos").value(4500))
        .andExpect(jsonPath("$.tarifaMediaHoraPesos").value(2800));
  }

  @Test
  @DisplayName(
      "CP-36h data.sql corrige una sola vez una base creada antes de CACS-30 y luego respeta los"
          + " cambios hechos por la API")
  void corrigeLaBaseAnterior() throws Exception {
    // Estado de una base anterior: sin tarifa de media hora, precios viejos y 4 Series S.
    jdbc.update(
        "update modalidades set tarifa_media_hora_pesos = null, tarifa_hora_pesos = 6000"
            + " where nombre = 'Xbox Series S'");
    jdbc.update(
        "insert into equipos (nombre, modalidad_id) select 'Xbox Series S #3', id from modalidades"
            + " where nombre = 'Xbox Series S'");
    ejecutarDataSql();
    mockMvc
        .perform(get("/api/equipos/" + equipo("Xbox Series S #1")))
        .andExpect(jsonPath("$.tarifaHoraPesos").value(5000))
        .andExpect(jsonPath("$.tarifaMediaHoraPesos").value(3000));
    mockMvc.perform(get("/api/equipos")).andExpect(jsonPath("$", hasSize(5)));

    // Una tarifa cambiada por la API ya no se pisa al reiniciar.
    jdbc.update("update modalidades set tarifa_hora_pesos = 5200 where nombre = 'Xbox Series S'");
    ejecutarDataSql();
    mockMvc
        .perform(get("/api/equipos/" + equipo("Xbox Series S #1")))
        .andExpect(jsonPath("$.tarifaHoraPesos").value(5200));
  }

  private void ejecutarDataSql() {
    entityManager.flush();
    new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
    entityManager.clear();
  }

  @Test
  @DisplayName("CP-36g Entrega el horario de los 7 días: todos de 3:00 a 9:00 p. m.")
  void entregaElHorario() throws Exception {
    mockMvc
        .perform(get("/api/horarios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(7)))
        .andExpect(jsonPath("$[0].diaSemana").value(1))
        .andExpect(jsonPath("$[*].horaApertura", everyItem(is("15:00:00"))))
        .andExpect(jsonPath("$[*].horaCierre", everyItem(is("21:00:00"))))
        .andExpect(jsonPath("$[0].horaApertura").value("15:00:00"))
        .andExpect(jsonPath("$[6].horaCierre").value("21:00:00"));
  }
}
