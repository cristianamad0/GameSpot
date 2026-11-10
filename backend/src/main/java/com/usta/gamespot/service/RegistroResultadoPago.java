package com.usta.gamespot.service;

import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.repository.PagoRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CACS-21 (HU-03): único punto donde se guarda el resultado de un cobro, venga del proveedor
 * simulado, del webhook de Wompi o de la consulta de una transacción.
 *
 * <p>Si el pago queda APROBADO, la base confirma la reserva (trigger
 * confirmar_reserva_al_aprobar_pago) y aquí se programan sus notificaciones (CACS-8), así que
 * salen igual con cualquier proveedor. Si la reserva no quedó CONFIRMADA (por ejemplo, ya había
 * expirado), no se programa nada.
 *
 * <p>Es idempotente: un pago que ya está APROBADO no vuelve a cambiar, aunque Wompi repita el
 * evento.
 */
@Component
@RequiredArgsConstructor
public class RegistroResultadoPago {

  private final PagoRepository pagoRepository;
  private final NotificacionService notificacionService;
  private final EntityManager entityManager;

  /**
   * Guarda el resultado del cobro en el pago. Debe llamarse dentro de una transacción que ya
   * bloqueó la reserva.
   *
   * @param pago pago que se actualiza
   * @param estado estado final según el proveedor
   * @param idTransaccion id de la transacción en el proveedor
   * @param metodoPago medio de pago usado
   * @param respuestaProveedor respuesta del proveedor como JSON; puede ser null
   * @return true si el pago cambió; false si ya estaba APROBADO y se ignoró
   */
  public boolean registrar(
      Pago pago,
      EstadoPago estado,
      String idTransaccion,
      String metodoPago,
      String respuestaProveedor) {
    if (pago.getEstado() == EstadoPago.APROBADO) {
      return false;
    }
    pago.setEstado(estado);
    pago.setIdTransaccionWompi(idTransaccion);
    pago.setMetodoPago(metodoPago);
    if (respuestaProveedor != null) {
      pago.setRespuestaWompi(respuestaProveedor);
    }
    pagoRepository.saveAndFlush(pago);

    if (estado == EstadoPago.APROBADO) {
      // CACS-8: el trigger pudo confirmar la reserva; se vuelve a leer para saberlo.
      Reserva reserva = pago.getReserva();
      entityManager.refresh(reserva);
      if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
        notificacionService.programarAlConfirmar(reserva, Instant.now());
      }
    }
    return true;
  }
}
