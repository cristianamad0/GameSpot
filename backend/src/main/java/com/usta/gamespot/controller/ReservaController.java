package com.usta.gamespot.controller;

import com.usta.gamespot.dto.CrearReservaRequest;
import com.usta.gamespot.dto.DisponibilidadResponse;
import com.usta.gamespot.dto.ReservaResponse;
import com.usta.gamespot.service.ReservaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-4 (HU-02): endpoints para que el cliente consulte la disponibilidad y reserve un equipo.
 * Exigen un token JWT válido (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

  private final ReservaService reservaService;

  /**
   * Reserva un equipo en uno o varios bloques seguidos de 30 minutos.
   *
   * @param request equipo, inicio y duración en minutos
   * @param usuario usuario autenticado
   * @return 201 con la reserva en PENDIENTE_PAGO, su precio, adelanto y fecha límite de pago; 400
   *     si el bloque o el horario no son válidos; 404 si el equipo no existe; 409 si el bloque ya
   *     está ocupado o el equipo está fuera de servicio
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReservaResponse crear(
      @Valid @RequestBody CrearReservaRequest request,
      @AuthenticationPrincipal UserDetails usuario) {
    return reservaService.crear(request, usuario.getUsername());
  }

  /**
   * CACS-17 (HU-03): devuelve una reserva propia, para la pantalla de pago del adelanto.
   *
   * @param id id de la reserva
   * @param usuario usuario autenticado
   * @return 200 con la reserva; 403 si es de otro usuario; 404 si no existe
   */
  @GetMapping("/{id}")
  public ReservaResponse obtener(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails usuario) {
    return reservaService.obtenerPropia(id, usuario.getUsername());
  }

  /**
   * Muestra los bloques libres y ocupados de un equipo en un día.
   *
   * @param equipoId id del equipo
   * @param fecha día en formato AAAA-MM-DD (hora de Bogotá)
   * @return 200 con los bloques del día; 404 si el equipo no existe
   */
  @GetMapping("/disponibilidad")
  public DisponibilidadResponse consultarDisponibilidad(
      @RequestParam Long equipoId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    return reservaService.consultarDisponibilidad(equipoId, fecha);
  }
}
