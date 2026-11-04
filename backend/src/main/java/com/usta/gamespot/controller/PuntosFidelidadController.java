package com.usta.gamespot.controller;

import com.usta.gamespot.dto.AjustePuntosRequest;
import com.usta.gamespot.dto.SaldoPuntosResponse;
import com.usta.gamespot.service.PuntosFidelidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-7 (HU-07): endpoints de puntos de fidelización. La consulta es para cualquier usuario
 * autenticado; el ajuste manual va bajo /api/admin/** y exige el rol ADMIN (ver SecurityConfig).
 */
@RestController
@RequiredArgsConstructor
public class PuntosFidelidadController {

  private final PuntosFidelidadService puntosFidelidadService;

  /**
   * Saldo e historial de puntos del usuario autenticado.
   *
   * @param usuario usuario autenticado
   * @return 200 con el saldo y los movimientos; 401 sin token
   */
  @GetMapping("/api/puntos")
  public SaldoPuntosResponse consultarPropios(@AuthenticationPrincipal UserDetails usuario) {
    return puntosFidelidadService.consultarPropios(usuario.getUsername());
  }

  /**
   * Registra un ajuste manual de puntos sobre un usuario.
   *
   * @param usuarioId id del usuario
   * @param request puntos (distintos de cero) y descripción obligatoria
   * @return 201 con el saldo y el historial actualizados; 400 si los datos no son válidos; 404 si
   *     el usuario no existe; 409 si el saldo quedaría negativo
   */
  @PostMapping("/api/admin/usuarios/{usuarioId}/puntos")
  @ResponseStatus(HttpStatus.CREATED)
  public SaldoPuntosResponse ajustar(
      @PathVariable Long usuarioId, @Valid @RequestBody AjustePuntosRequest request) {
    return puntosFidelidadService.ajustar(usuarioId, request.puntos(), request.descripcion());
  }
}
