package com.usta.gamespot.reservas.controller;

import com.usta.gamespot.reservas.dto.BloqueOcupadoResponse;
import com.usta.gamespot.reservas.dto.CrearReservaRequest;
import com.usta.gamespot.reservas.dto.ReservaResponse;
import com.usta.gamespot.reservas.security.FiltroJwt;
import com.usta.gamespot.reservas.service.ReservaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-26: endpoints de reservas. Todos exigen un JWT salvo la ocupación, que es pública (ver
 * FiltroJwt). El correo y el rol del usuario llegan como atributos que deja el filtro.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

  private final ReservaService reservaService;

  /**
   * Crea una reserva en PENDIENTE_PAGO.
   *
   * @param request equipo, tarifa, inicio y duración
   * @param email correo del usuario autenticado
   * @return 201 con la reserva; 400 datos o bloque inválidos; 401 sin token; 409 bloque ocupado
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReservaResponse crear(
      @Valid @RequestBody CrearReservaRequest request,
      @RequestAttribute(FiltroJwt.EMAIL) String email) {
    return reservaService.crear(request, email);
  }

  /**
   * Reservas del usuario autenticado.
   *
   * @param email correo del usuario autenticado
   * @return 200 con sus reservas, de la más reciente a la más antigua
   */
  @GetMapping("/mias")
  public List<ReservaResponse> mias(@RequestAttribute(FiltroJwt.EMAIL) String email) {
    return reservaService.listarPropias(email);
  }

  /**
   * Bloques ocupados de un equipo en un día. Público.
   *
   * @param equipoId id del equipo
   * @param fecha día en formato AAAA-MM-DD (hora de Bogotá)
   * @return 200 con los intervalos ocupados
   */
  @GetMapping("/ocupacion")
  public List<BloqueOcupadoResponse> ocupacion(
      @RequestParam Long equipoId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    return reservaService.ocupacion(equipoId, fecha);
  }

  /**
   * Una reserva propia.
   *
   * @param id id de la reserva
   * @param email correo del usuario autenticado
   * @param rol rol del usuario autenticado
   * @return 200 con la reserva; 403 si es de otro usuario; 404 si no existe
   */
  @GetMapping("/{id}")
  public ReservaResponse obtener(
      @PathVariable Long id,
      @RequestAttribute(FiltroJwt.EMAIL) String email,
      @RequestAttribute(FiltroJwt.ROL) String rol) {
    return reservaService.obtener(id, email, rol);
  }

  /**
   * Paga el adelanto (proveedor simulado) y confirma la reserva.
   *
   * @param id id de la reserva
   * @param email correo del usuario autenticado
   * @param rol rol del usuario autenticado
   * @return 200 con la reserva CONFIRMADA; 403, 404 o 409 según el caso
   */
  @PatchMapping("/{id}/pago")
  public ReservaResponse pagar(
      @PathVariable Long id,
      @RequestAttribute(FiltroJwt.EMAIL) String email,
      @RequestAttribute(FiltroJwt.ROL) String rol) {
    return reservaService.pagarAdelanto(id, email, rol);
  }

  /**
   * Cancela una reserva (cancelación lógica).
   *
   * @param id id de la reserva
   * @param email correo del usuario autenticado
   * @param rol rol del usuario autenticado
   * @return 200 con la reserva CANCELADA; 403, 404 o 409 según el caso
   */
  @PatchMapping("/{id}/cancelacion")
  public ReservaResponse cancelar(
      @PathVariable Long id,
      @RequestAttribute(FiltroJwt.EMAIL) String email,
      @RequestAttribute(FiltroJwt.ROL) String rol) {
    return reservaService.cancelar(id, email, rol);
  }
}
