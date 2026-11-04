package com.usta.gamespot.controller;

import com.usta.gamespot.dto.CancelacionReservaRequest;
import com.usta.gamespot.dto.ReservaAdminResponse;
import com.usta.gamespot.dto.ReservaExpiradaConPagoResponse;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.service.AdminReservasService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-6 (HU-06): endpoints del panel de administración para gestionar las reservas. Toda ruta
 * bajo /api/admin/** exige el rol ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/reservas")
@RequiredArgsConstructor
public class AdminReservasController {

  private final AdminReservasService adminReservasService;

  /**
   * Lista las reservas de un día, con filtros opcionales por equipo y por estado.
   *
   * @param fecha día en formato AAAA-MM-DD (hora de Bogotá)
   * @param equipoId id del equipo, opcional
   * @param estado estado de la reserva, opcional (por ejemplo, CONFIRMADA)
   * @return 200 con las reservas ordenadas por hora de inicio
   */
  @GetMapping
  public List<ReservaAdminResponse> listarDelDia(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @RequestParam(required = false) Long equipoId,
      @RequestParam(required = false) EstadoReserva estado) {
    return adminReservasService.listarDelDia(fecha, equipoId, estado);
  }

  /**
   * Cancela una reserva PENDIENTE_PAGO o CONFIRMADA y libera su bloque.
   *
   * @param id id de la reserva
   * @param request motivo de la cancelación
   * @return 200 con la reserva cancelada; 400 sin motivo; 404 si no existe; 409 si su estado no
   *     permite cancelarla
   */
  @PostMapping("/{id}/cancelar")
  public ReservaAdminResponse cancelar(
      @PathVariable Long id, @Valid @RequestBody CancelacionReservaRequest request) {
    return adminReservasService.cancelar(id, request.motivo());
  }

  /**
   * Marca como COMPLETADA una reserva CONFIRMADA; el cliente recibe sus puntos de fidelidad.
   *
   * @param id id de la reserva
   * @return 200 con la reserva completada; 404 si no existe; 409 si no está CONFIRMADA
   */
  @PostMapping("/{id}/completar")
  public ReservaAdminResponse completar(@PathVariable Long id) {
    return adminReservasService.completar(id);
  }

  /**
   * Lista las reservas que expiraron con un pago aprobado, para resolverlas a mano.
   *
   * @return 200 con los casos pendientes de resolver
   */
  @GetMapping("/expiradas-con-pago")
  public List<ReservaExpiradaConPagoResponse> listarExpiradasConPagoAprobado() {
    return adminReservasService.listarExpiradasConPagoAprobado();
  }
}
