package com.usta.gamespot.controller;

import com.usta.gamespot.dto.PagoAnticipoRequest;
import com.usta.gamespot.dto.PagoAnticipoResponse;
import com.usta.gamespot.service.PagoAnticipoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-5 (HU-03): endpoint para que el cliente pague el adelanto de su reserva. Exige un token JWT
 * válido (ver SecurityConfig).
 */
@RestController
@RequiredArgsConstructor
public class PagoAnticipoController {

  private final PagoAnticipoService pagoAnticipoService;

  /**
   * Paga el adelanto de una reserva propia con el proveedor de anticipos.
   *
   * @param id id de la reserva
   * @param usuario usuario autenticado
   * @param request resultado que debe dar el proveedor simulado; el cuerpo es opcional
   * @return 201 con el pago (APROBADO o RECHAZADO) y el estado de la reserva; 403 si la reserva es
   *     de otro usuario; 404 si no existe; 409 si no está PENDIENTE_PAGO o ya venció su plazo
   */
  @PostMapping("/api/reservas/{id}/pago")
  @ResponseStatus(HttpStatus.CREATED)
  public PagoAnticipoResponse pagarAdelanto(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails usuario,
      @RequestBody(required = false) PagoAnticipoRequest request) {
    return pagoAnticipoService.pagarAdelanto(
        id, usuario.getUsername(), request == null ? null : request.resultadoSimulado());
  }
}
