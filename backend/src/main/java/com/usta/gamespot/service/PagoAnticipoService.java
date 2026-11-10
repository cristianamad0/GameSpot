package com.usta.gamespot.service;

import com.usta.gamespot.config.PropiedadesPagos;
import com.usta.gamespot.config.ProveedorPagos;
import com.usta.gamespot.dto.PagoAnticipoResponse;
import com.usta.gamespot.dto.ResultadoSimulado;
import com.usta.gamespot.exception.PlazoPagoVencidoException;
import com.usta.gamespot.exception.ProveedorPagosNoDisponibleException;
import com.usta.gamespot.exception.ReservaAjenaException;
import com.usta.gamespot.exception.ReservaNoEncontradaException;
import com.usta.gamespot.exception.TransicionReservaInvalidaException;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.service.ProveedorAnticipos.ResultadoCobro;
import com.usta.gamespot.service.ProveedorAnticipos.SolicitudCobro;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-5 (HU-03): pago del adelanto de una reserva con el proveedor de anticipos.
 *
 * <p>El servicio registra el pago y guarda la respuesta del proveedor. Quien confirma la reserva
 * es la base de datos (trigger confirmar_reserva_al_aprobar_pago), y solo si el pago cubre el
 * adelanto; la base también impide dos pagos aprobados para la misma reserva.
 */
@Service
@RequiredArgsConstructor
public class PagoAnticipoService {

  private final ReservaRepository reservaRepository;
  private final PagoRepository pagoRepository;
  private final ProveedorAnticipos proveedorAnticipos;
  private final RegistroResultadoPago registroResultadoPago;
  private final PropiedadesPagos propiedadesPagos;
  private final EntityManager entityManager;

  /**
   * Cobra el adelanto de una reserva propia que sigue en PENDIENTE_PAGO. Si el pago se aprueba, la
   * reserva queda CONFIRMADA; si se rechaza, sigue PENDIENTE_PAGO y se puede intentar de nuevo
   * mientras no venza el plazo.
   *
   * @param reservaId id de la reserva
   * @param emailUsuario correo del usuario autenticado
   * @param resultadoSimulado resultado que debe dar el proveedor simulado; puede ser null
   * @return el pago registrado y el estado de la reserva después del pago
   * @throws ReservaNoEncontradaException si la reserva no existe
   * @throws ReservaAjenaException si la reserva es de otro usuario
   * @throws TransicionReservaInvalidaException si la reserva no está en PENDIENTE_PAGO
   * @throws PlazoPagoVencidoException si el plazo para pagar ya venció
   * @throws ProveedorPagosNoDisponibleException si el proveedor activo no es el simulado
   */
  @Transactional
  public PagoAnticipoResponse pagarAdelanto(
      Long reservaId, String emailUsuario, ResultadoSimulado resultadoSimulado) {
    // CACS-21: con Wompi activo, nadie puede aprobar un pago con el proveedor simulado.
    if (propiedadesPagos.proveedor() != ProveedorPagos.SIMULADO) {
      throw new ProveedorPagosNoDisponibleException(propiedadesPagos.proveedor());
    }
    Reserva reserva =
        reservaRepository
            .buscarParaPagar(reservaId)
            .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
    validarQueSePuedePagar(reserva, emailUsuario);

    Pago pago =
        pagoRepository.saveAndFlush(
            Pago.builder()
                .reserva(reserva)
                .referencia(nuevaReferencia(reservaId))
                .montoCentavos(reserva.getAdelantoPesos() * 100L)
                .build());

    ResultadoCobro resultado =
        proveedorAnticipos.cobrar(
            new SolicitudCobro(pago.getReferencia(), pago.getMontoCentavos(), resultadoSimulado));
    registroResultadoPago.registrar(
        pago, resultado.estado(), resultado.idTransaccion(), resultado.metodoPago(), null);

    // El trigger de la base pudo confirmar la reserva: se vuelve a leer.
    entityManager.refresh(reserva);
    return PagoAnticipoResponse.de(pago, reserva);
  }

  /** Reglas comunes para iniciar un pago (también las usa {@link PagoWompiService}). */
  static void validarQueSePuedePagar(Reserva reserva, String emailUsuario) {
    if (!reserva.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
      throw new ReservaAjenaException(reserva.getId());
    }
    if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
      throw new TransicionReservaInvalidaException(reserva.getId(), reserva.getEstado(), "pagar");
    }
    if (!reserva.getExpiraEn().isAfter(Instant.now())) {
      throw new PlazoPagoVencidoException(reserva.getId());
    }
  }

  /** Referencia única para el proveedor, por ejemplo GS-15-1A2B3C4D5E6F. */
  static String nuevaReferencia(Long reservaId) {
    String aleatorio = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    return "GS-" + reservaId + "-" + aleatorio.toUpperCase(Locale.ROOT);
  }
}
