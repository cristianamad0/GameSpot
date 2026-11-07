package com.usta.gamespot;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de la búsqueda de usuarios del panel (HU-06, CACS-18), que usa el ajuste de puntos del
 * frontend. Llaman al endpoint con tokens JWT reales. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class AdminUsuariosTests {

  private static final String RUTA = "/api/admin/usuarios";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UsuarioDetailsService usuarioDetailsService;
  @Autowired private UsuarioRepository usuarioRepository;

  private String tokenAdmin;
  private String tokenCliente;

  @BeforeEach
  void prepararDatos() {
    tokenAdmin = "Bearer " + tokenDe(crearUsuario("Admin", "admin@gamespot.test", Rol.ADMIN));
    tokenCliente =
        "Bearer " + tokenDe(crearUsuario("Laura", "laura.gamer@gamespot.test", Rol.CLIENTE));
    crearUsuario("Pedro", "pedro.gamer@gamespot.test", Rol.CLIENTE);
    crearUsuario("Ana", "ana@otrodominio.test", Rol.CLIENTE);
  }

  private Usuario crearUsuario(String nombre, String email, Rol rol) {
    return usuarioRepository.saveAndFlush(
        Usuario.builder()
            .nombre(nombre)
            .email(email)
            .passwordHash("hash-de-prueba")
            .rol(rol)
            .build());
  }

  private String tokenDe(Usuario usuario) {
    return jwtService.generarToken(usuarioDetailsService.loadUserByUsername(usuario.getEmail()));
  }

  private ResultActions buscar(String texto, String token) throws Exception {
    return mockMvc.perform(get(RUTA).param("email", texto).header("Authorization", token));
  }

  @Test
  @DisplayName("CP-28a El admin encuentra clientes por parte del correo, sin distinguir mayúsculas")
  void buscaPorParteDelCorreo() throws Exception {
    buscar("GAMER", tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].email").value("laura.gamer@gamespot.test"))
        .andExpect(jsonPath("$[0].nombre").value("Laura"))
        .andExpect(jsonPath("$[0].rol").value("CLIENTE"))
        .andExpect(jsonPath("$[0].puntosFidelidad").value(0))
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
        .andExpect(jsonPath("$[1].email").value("pedro.gamer@gamespot.test"));
  }

  @Test
  @DisplayName("CP-28b Devuelve como máximo 10 usuarios")
  void devuelveComoMaximoDiez() throws Exception {
    for (int i = 1; i <= 12; i++) {
      crearUsuario("Cliente " + i, "masivo" + i + "@gamespot.test", Rol.CLIENTE);
    }

    buscar("masivo", tokenAdmin).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(10)));
  }

  @Test
  @DisplayName("CP-28c Sin coincidencias responde una lista vacía")
  void sinCoincidenciasListaVacia() throws Exception {
    buscar("nadie-con-este-correo", tokenAdmin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @DisplayName("CP-28d Un texto de menos de 3 caracteres responde 400")
  void textoCortoResponde400() throws Exception {
    buscar("ab", tokenAdmin).andExpect(status().isBadRequest());
    buscar("   ", tokenAdmin).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("CP-28e Un CLIENTE recibe 403 y sin token 401")
  void soloAdmin() throws Exception {
    buscar("gamer", tokenCliente).andExpect(status().isForbidden());
    mockMvc.perform(get(RUTA).param("email", "gamer")).andExpect(status().isUnauthorized());
  }
}
