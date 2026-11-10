package com.usta.gamespot.catalogo.controller;

import com.usta.gamespot.catalogo.dto.EquipoRequest;
import com.usta.gamespot.catalogo.dto.EquipoResponse;
import com.usta.gamespot.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-25: CRUD de equipos. Las consultas son públicas; crear, actualizar y eliminar exigen un JWT
 * de administrador (ver FiltroJwt).
 */
@RestController
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

  private final CatalogoService catalogoService;

  /**
   * Lista los equipos.
   *
   * @return 200 con los equipos
   */
  @GetMapping
  public List<EquipoResponse> listar() {
    return catalogoService.listarEquipos();
  }

  /**
   * Consulta un equipo.
   *
   * @param id id del equipo
   * @return 200 con el equipo; 404 si no existe
   */
  @GetMapping("/{id}")
  public EquipoResponse obtener(@PathVariable Long id) {
    return catalogoService.obtenerEquipo(id);
  }

  /**
   * Crea un equipo.
   *
   * @param request datos del equipo
   * @return 201 con el equipo; 400 datos inválidos; 404 modalidad inexistente; 409 nombre repetido
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EquipoResponse crear(@Valid @RequestBody EquipoRequest request) {
    return catalogoService.crearEquipo(request);
  }

  /**
   * Actualiza un equipo (nombre, modalidad y si está disponible).
   *
   * @param id id del equipo
   * @param request datos nuevos
   * @return 200 con el equipo; 400, 404 o 409 según el caso
   */
  @PutMapping("/{id}")
  public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest request) {
    return catalogoService.actualizarEquipo(id, request);
  }

  /**
   * Elimina un equipo.
   *
   * @param id id del equipo
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    catalogoService.eliminarEquipo(id);
  }
}
