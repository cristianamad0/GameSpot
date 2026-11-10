package com.usta.gamespot.controller;

import com.usta.gamespot.dto.DisponibilidadEquipoRequest;
import com.usta.gamespot.dto.EquipoAdminResponse;
import com.usta.gamespot.dto.ModalidadAdminResponse;
import com.usta.gamespot.dto.ModalidadRequest;
import com.usta.gamespot.service.AdminEquiposService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-22 (HU-06): endpoints del panel para gestionar equipos y tarifas. Van bajo /api/admin/**,
 * así que exigen el rol ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminEquiposController {

  private final AdminEquiposService adminEquiposService;

  /**
   * Lista todos los equipos con su modalidad, tarifa, estado y reservas activas futuras.
   *
   * @return 200 con los equipos
   */
  @GetMapping("/equipos")
  public List<EquipoAdminResponse> listarEquipos() {
    return adminEquiposService.listarEquipos();
  }

  /**
   * Pone un equipo fuera de servicio o lo vuelve a habilitar.
   *
   * @param id id del equipo
   * @param request si queda disponible
   * @return 200 con el equipo actualizado; 400 sin el campo; 404 si no existe
   */
  @PutMapping("/equipos/{id}/disponibilidad")
  public EquipoAdminResponse cambiarDisponibilidad(
      @PathVariable Long id, @Valid @RequestBody DisponibilidadEquipoRequest request) {
    return adminEquiposService.cambiarDisponibilidad(id, request.estaDisponible());
  }

  /**
   * Lista las modalidades con su tarifa y cuántos equipos tienen.
   *
   * @return 200 con las modalidades
   */
  @GetMapping("/modalidades")
  public List<ModalidadAdminResponse> listarModalidades() {
    return adminEquiposService.listarModalidades();
  }

  /**
   * Cambia la tarifa de una modalidad y la activa o desactiva.
   *
   * @param id id de la modalidad
   * @param request nueva tarifa y si está activa
   * @return 200 con la modalidad actualizada; 400 si la tarifa no es positiva; 404 si no existe
   */
  @PutMapping("/modalidades/{id}")
  public ModalidadAdminResponse actualizarModalidad(
      @PathVariable Long id, @Valid @RequestBody ModalidadRequest request) {
    return adminEquiposService.actualizarModalidad(id, request);
  }
}
