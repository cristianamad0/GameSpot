package com.usta.gamespot.portal.cliente;

import com.usta.gamespot.portal.dto.BloqueOcupadoDto;
import com.usta.gamespot.portal.dto.CrearReservaDto;
import com.usta.gamespot.portal.dto.LoginRequest;
import com.usta.gamespot.portal.dto.ReservaDto;
import com.usta.gamespot.portal.dto.TokenDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * CACS-27: llamadas HTTP al proveedor de reservas (puerto 8092) con RestClient. Las operaciones
 * protegidas reenvían el encabezado Authorization que mandó el navegador.
 */
@Component
public class ReservasCliente {

  static final String SERVICIO = "reservas";

  private final RestClient restClient;

  /**
   * Crea el cliente.
   *
   * @param restClient RestClient con la URL base del proveedor de reservas
   */
  public ReservasCliente(@Qualifier("clienteReservas") RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * POST /api/auth/login.
   *
   * @param request correo y contraseña
   * @return el token
   */
  public TokenDto login(LoginRequest request) {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () -> restClient.post().uri("/api/auth/login").body(request).retrieve().body(TokenDto.class));
  }

  /**
   * GET /api/reservas/ocupacion (público).
   *
   * @param equipoId id del equipo
   * @param fecha día
   * @return intervalos ocupados
   */
  public List<BloqueOcupadoDto> ocupacion(Long equipoId, LocalDate fecha) {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .get()
                .uri(
                    u ->
                        u.path("/api/reservas/ocupacion")
                            .queryParam("equipoId", equipoId)
                            .queryParam("fecha", fecha)
                            .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<BloqueOcupadoDto>>() {}));
  }

  /**
   * POST /api/reservas.
   *
   * @param reserva datos completos de la reserva
   * @param autorizacion encabezado Authorization del navegador
   * @return la reserva creada
   */
  public ReservaDto crear(CrearReservaDto reserva, String autorizacion) {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .post()
                .uri("/api/reservas")
                .header("Authorization", autorizacion)
                .body(reserva)
                .retrieve()
                .body(ReservaDto.class));
  }

  /**
   * GET /api/reservas/mias.
   *
   * @param autorizacion encabezado Authorization del navegador
   * @return las reservas del usuario
   */
  public List<ReservaDto> mias(String autorizacion) {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .get()
                .uri("/api/reservas/mias")
                .header("Authorization", autorizacion)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ReservaDto>>() {}));
  }

  /**
   * PATCH /api/reservas/{id}/pago o /cancelacion.
   *
   * @param id id de la reserva
   * @param accion "pago" o "cancelacion"
   * @param autorizacion encabezado Authorization del navegador
   * @return la reserva actualizada
   */
  public ReservaDto cambiarEstado(Long id, String accion, String autorizacion) {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .patch()
                .uri("/api/reservas/{id}/{accion}", id, accion)
                .header("Authorization", autorizacion)
                .retrieve()
                .body(ReservaDto.class));
  }

  /**
   * Comprueba si el proveedor responde.
   *
   * @return true si respondió a la consulta de ocupación
   */
  public boolean estaDisponible() {
    try {
      ocupacion(1L, LocalDate.now());
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }
}
