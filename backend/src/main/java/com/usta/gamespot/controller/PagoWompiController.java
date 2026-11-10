package com.usta.gamespot.controller;

import com.usta.gamespot.dto.CheckoutWompiResponse;
import com.usta.gamespot.dto.PagoAnticipoResponse;
import com.usta.gamespot.dto.ProveedorPagosResponse;
import com.usta.gamespot.service.PagoWompiService;
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
import tools.jackson.databind.JsonNode;

/**
 * CACS-21 (HU-03): pago del adelanto con Wompi. El webhook es público (lo llama Wompi, sin token)
 * y se protege con el checksum del evento; los demás endpoints exigen un token JWT válido.
 */
@RestController
@RequiredArgsConstructor
public class PagoWompiController {

  private final PagoWompiService pagoWompiService;

  /**
   * Proveedor de pagos activo, para que el frontend sepa qué flujo mostrar.
   *
   * @return 200 con SIMULADO o WOMPI
   */
  @GetMapping("/api/pagos/proveedor")
  public ProveedorPagosResponse proveedor() {
    return new ProveedorPagosResponse(pagoWompiService.proveedorActivo());
  }

  /**
   * Prepara el pago del adelanto en el Web Checkout de Wompi.
   *
   * @param id id de la reserva
   * @param usuario usuario autenticado
   * @return 201 con la URL del checkout; 403 si la reserva es de otro usuario; 404 si no existe;
   *     409 si no está PENDIENTE_PAGO, ya venció su plazo o el proveedor activo no es Wompi
   */
  @PostMapping("/api/reservas/{id}/pago/wompi")
  @ResponseStatus(HttpStatus.CREATED)
  public CheckoutWompiResponse iniciarCheckout(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails usuario) {
    return pagoWompiService.iniciarCheckout(id, usuario.getUsername());
  }

  /**
   * Consulta en Wompi la transacción con la que volvió el cliente y registra su resultado.
   *
   * @param id id de la reserva
   * @param idTransaccion id de la transacción en Wompi
   * @param usuario usuario autenticado
   * @return 200 con el pago y el estado de la reserva; 403 si la reserva es de otro usuario; 404
   *     si la reserva o la transacción no existen; 409 si el proveedor activo no es Wompi
   */
  @GetMapping("/api/reservas/{id}/pago/wompi/{idTransaccion}")
  public PagoAnticipoResponse consultarTransaccion(
      @PathVariable Long id,
      @PathVariable String idTransaccion,
      @AuthenticationPrincipal UserDetails usuario) {
    return pagoWompiService.consultarTransaccion(id, usuario.getUsername(), idTransaccion);
  }

  /**
   * Webhook de eventos de Wompi. Responde 200 si el checksum es válido (aunque el evento se
   * ignore) y 401 si no lo es.
   *
   * @param evento cuerpo del evento
   */
  @PostMapping("/api/pagos/wompi/eventos")
  public void recibirEvento(@RequestBody JsonNode evento) {
    pagoWompiService.procesarEvento(evento);
  }
}
