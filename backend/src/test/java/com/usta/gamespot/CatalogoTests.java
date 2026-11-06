package com.usta.gamespot;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.security.JwtService;
import com.usta.gamespot.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del catálogo de equipos para la pantalla de reservar (HU-02, CACS-19). Usan los equipos,
 * tarifas y configuración reales que carga esquema.sql. Cada prueba corre en una transacción que
 * se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class CatalogoTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;

  private String tokenCliente;

  @BeforeEach
  void prepararCliente() {
    Usuario cliente =
        usuarioRepository.saveAndFlush(
            Usuario.builder()
                .nombre("Cliente de prueba")
                .email("cliente@gamespot.test")
                .passwordHash("hash-de-prueba")
                .rol(Rol.CLIENTE)
                .build());
    tokenCliente =
        "Bearer "
            + jwtService.generarToken(
                usuarioDetailsService.loadUserByUsername(cliente.getEmail()));
  }

  @Test
  @DisplayName("CP-31a El catálogo trae los equipos con modalidad y tarifa, y el adelanto del 50 %")
  void catalogoConEquiposYCondicionesDePago() throws Exception {
    mockMvc
        .perform(get("/api/catalogo").header("Authorization", tokenCliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.porcentajeAdelanto").value(50))
        .andExpect(jsonPath("$.minutosEsperaPago").value(15))
        .andExpect(jsonPath("$.equipos", hasSize(6)))
        // Del más económico al más costoso.
        .andExpect(jsonPath("$.equipos[0].nombre").value("Xbox 360 #1"))
        .andExpect(jsonPath("$.equipos[0].modalidad").value("Xbox 360"))
        .andExpect(jsonPath("$.equipos[0].tarifaHoraPesos").value(5000))
        .andExpect(jsonPath("$.equipos[0].estaDisponible").value(true))
        .andExpect(jsonPath("$.equipos[5].nombre").value("Sala VIP Xbox Series X"))
        .andExpect(jsonPath("$.equipos[5].tarifaHoraPesos").value(7000));
  }

  @Test
  @DisplayName("CP-31b Un equipo fuera de servicio aparece en el catálogo como no disponible")
  void equipoFueraDeServicioNoEstaDisponible() throws Exception {
    Equipo xbox360 =
        equipoRepository.findAll().stream()
            .filter(e -> e.getNombre().equals("Xbox 360 #1"))
            .findFirst()
            .orElseThrow();
    xbox360.setEstaDisponible(false);
    equipoRepository.saveAndFlush(xbox360);

    mockMvc
        .perform(get("/api/catalogo").header("Authorization", tokenCliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.equipos[0].nombre").value("Xbox 360 #1"))
        .andExpect(jsonPath("$.equipos[0].estaDisponible").value(false));
  }

  @Test
  @DisplayName("CP-31c Sin token, el catálogo responde 401")
  void sinTokenResponde401() throws Exception {
    mockMvc.perform(get("/api/catalogo")).andExpect(status().isUnauthorized());
  }
}
