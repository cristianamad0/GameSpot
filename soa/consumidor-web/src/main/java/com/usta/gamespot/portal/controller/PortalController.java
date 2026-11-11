package com.usta.gamespot.portal.controller;

import com.usta.gamespot.portal.dto.DisponibilidadResponse;
import com.usta.gamespot.portal.dto.EquipoDto;
import com.usta.gamespot.portal.dto.EstadoServiciosResponse;
import com.usta.gamespot.portal.dto.HorarioDto;
import com.usta.gamespot.portal.dto.LoginRequest;
import com.usta.gamespot.portal.dto.ReservaDto;
import com.usta.gamespot.portal.dto.ReservaPortalRequest;
import com.usta.gamespot.portal.dto.TokenDto;
import com.usta.gamespot.portal.service.PortalService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-27: backend de la consumidora. La página web (static/index.html) llama a estos endpoints
 * con Fetch; ellos llaman a los proveedores con RestClient. Las operaciones protegidas reciben el
 * encabezado Authorization del navegador y lo reenvían al proveedor que valida el JWT.
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

  private final PortalService portalService;

  /**
   * Estado de los proveedores.
   *
   * @return 200 con DISPONIBLE o NO_DISPONIBLE para cada uno
   */
  @GetMapping("/estado")
  public EstadoServiciosResponse estado() {
    return portalService.estadoServicios();
  }

  /**
   * Equipos del catálogo (proveedor 1).
   *
   * @return 200 con los equipos; 503 si el catálogo no responde
   */
  @GetMapping("/equipos")
  public List<EquipoDto> equipos() {
    return portalService.equipos();
  }

  /**
   * CACS-29: horario de atención del catálogo (proveedor 1), para mostrarlo en la portada.
   *
   * @return 200 con el horario de los días que atiende; 503 si el catálogo no responde
   */
  @GetMapping("/horarios")
  public List<HorarioDto> horarios() {
    return portalService.horarios();
  }

  /**
   * Disponibilidad de un equipo en un día (operación integrada).
   *
   * @param id id del equipo
   * @param fecha día en formato AAAA-MM-DD
   * @return 200 con los bloques; 404 si el equipo no existe; 503 si un proveedor no responde
   */
  @GetMapping("/equipos/{id}/disponibilidad")
  public DisponibilidadResponse disponibilidad(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    return portalService.disponibilidad(id, fecha);
  }

  /**
   * Inicio de sesión (proveedor 2).
   *
   * @param request correo y contraseña
   * @return 200 con el token; 400 datos inválidos; 401 credenciales incorrectas
   */
  @PostMapping("/login")
  public TokenDto login(@Valid @RequestBody LoginRequest request) {
    return portalService.login(request);
  }

  /**
   * Crea una reserva (operación integrada).
   *
   * @param request equipo, fecha, hora y duración
   * @param autorizacion encabezado Authorization con el JWT
   * @return 201 con la reserva; 400, 401, 409 o 503 según el caso
   */
  @PostMapping("/reservas")
  @ResponseStatus(HttpStatus.CREATED)
  public ReservaDto reservar(
      @Valid @RequestBody ReservaPortalRequest request,
      @RequestHeader("Authorization") String autorizacion) {
    return portalService.reservar(request, autorizacion);
  }

  /**
   * Reservas del usuario (proveedor 2).
   *
   * @param autorizacion encabezado Authorization con el JWT
   * @return 200 con sus reservas; 401 sin sesión
   */
  @GetMapping("/reservas/mias")
  public List<ReservaDto> misReservas(@RequestHeader("Authorization") String autorizacion) {
    return portalService.misReservas(autorizacion);
  }

  /**
   * Paga el adelanto de una reserva (proveedor 2).
   *
   * @param id id de la reserva
   * @param autorizacion encabezado Authorization con el JWT
   * @return 200 con la reserva CONFIRMADA
   */
  @PatchMapping("/reservas/{id}/pago")
  public ReservaDto pagar(
      @PathVariable Long id, @RequestHeader("Authorization") String autorizacion) {
    return portalService.cambiarEstadoReserva(id, "pago", autorizacion);
  }

  /**
   * Cancela una reserva (proveedor 2).
   *
   * @param id id de la reserva
   * @param autorizacion encabezado Authorization con el JWT
   * @return 200 con la reserva CANCELADA
   */
  @PatchMapping("/reservas/{id}/cancelacion")
  public ReservaDto cancelar(
      @PathVariable Long id, @RequestHeader("Authorization") String autorizacion) {
    return portalService.cambiarEstadoReserva(id, "cancelacion", autorizacion);
  }

  /**
   * Pone un equipo fuera de servicio o lo habilita (proveedor 1, rol ADMIN).
   *
   * @param id id del equipo
   * @param cuerpo {"estaDisponible": true o false}
   * @param autorizacion encabezado Authorization con el JWT
   * @return 200 con el equipo; 401 sin sesión; 403 sin rol ADMIN
   */
  @PatchMapping("/equipos/{id}/estado")
  public EquipoDto cambiarEstadoEquipo(
      @PathVariable Long id,
      @RequestBody Map<String, Boolean> cuerpo,
      @RequestHeader("Authorization") String autorizacion) {
    boolean estaDisponible = Boolean.TRUE.equals(cuerpo.get("estaDisponible"));
    return portalService.cambiarDisponibilidadEquipo(id, estaDisponible, autorizacion);
  }
}
