package com.usta.gamespot.service;

import com.usta.gamespot.config.PropiedadesPagos;
import com.usta.gamespot.config.ProveedorPagos;
import com.usta.gamespot.dto.CheckoutWompiResponse;
import com.usta.gamespot.dto.PagoAnticipoResponse;
import com.usta.gamespot.dto.TransaccionWompi;
import com.usta.gamespot.exception.FirmaWompiInvalidaException;
import com.usta.gamespot.exception.PlazoPagoVencidoException;
import com.usta.gamespot.exception.ProveedorPagosNoDisponibleException;
import com.usta.gamespot.exception.ReservaAjenaException;
import com.usta.gamespot.exception.ReservaNoEncontradaException;
import com.usta.gamespot.exception.TransaccionWompiNoEncontradaException;
import com.usta.gamespot.exception.TransicionReservaInvalidaException;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import jakarta.persistence.EntityManager;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * CACS-21 (HU-03): pago del adelanto con el Web Checkout de Wompi.
 *
 * <ol>
 *   <li>El cliente pide pagar: se crea el pago en PENDIENTE (o se reutiliza el que ya tenía) y se
 *       arma la URL del checkout con el monto y la referencia firmados.
 *   <li>El cliente paga en Wompi. Wompi avisa el resultado con un evento al webhook, que se acepta
 *       solo si su checksum es válido.
 *   <li>Al volver del checkout, el frontend también puede pedir que se consulte la transacción en
 *       Wompi. Así el pago se registra aunque el webhook no llegue (por ejemplo, en local).
 * </ol>
 *
 * <p>El resultado se guarda con {@link RegistroResultadoPago} y la base confirma la reserva, igual
 * que con el proveedor simulado.
 *
 * <p>CACS-24: Wompi solo firma algunos campos del evento (normalmente id, estado y monto), no la
 * referencia. Por eso, si la referencia no viene firmada, la transacción se vuelve a leer de la API
 * de Wompi por su id, que sí va firmado, y se usan esos datos. Un segundo pago aprobado de una
 * reserva que ya tenía uno queda en ERROR (para devolverlo a mano) en vez de romper el webhook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoWompiService {

  private static final String MONEDA = "COP";
  private static final String EVENTO_TRANSACCION = "transaction.updated";
  private static final String PROPIEDAD_REFERENCIA = "transaction.reference";

  private final ReservaRepository reservaRepository;
  private final PagoRepository pagoRepository;
  private final RegistroResultadoPago registroResultadoPago;
  private final ClienteWompi clienteWompi;
  private final PropiedadesPagos propiedadesPagos;
  private final EntityManager entityManager;

  /**
   * Proveedor de pagos activo.
   *
   * @return SIMULADO o WOMPI
   */
  public ProveedorPagos proveedorActivo() {
    return propiedadesPagos.proveedor();
  }

  /**
   * Devuelve los datos del Web Checkout. Si la reserva ya tiene un pago PENDIENTE de un checkout
   * anterior, lo reutiliza (misma referencia), para que un segundo clic no abra un segundo cobro;
   * si no, crea uno. Aplica las mismas reglas que el pago simulado: reserva propia, en
   * PENDIENTE_PAGO y dentro del plazo.
   *
   * @param reservaId id de la reserva
   * @param emailUsuario correo del usuario autenticado
   * @return la URL del checkout y los datos firmados
   * @throws ProveedorPagosNoDisponibleException si el proveedor activo no es Wompi
   * @throws ReservaNoEncontradaException si la reserva no existe
   * @throws ReservaAjenaException si la reserva es de otro usuario
   * @throws TransicionReservaInvalidaException si la reserva no está en PENDIENTE_PAGO
   * @throws PlazoPagoVencidoException si el plazo para pagar ya venció
   */
  @Transactional
  public CheckoutWompiResponse iniciarCheckout(Long reservaId, String emailUsuario) {
    exigirWompi();
    Reserva reserva = bloquearReserva(reservaId);
    PagoAnticipoService.validarQueSePuedePagar(reserva, emailUsuario);

    long montoCentavos = reserva.getAdelantoPesos() * 100L;
    Pago pago =
        pagoRepository.findByReservaId(reservaId).stream()
            .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
            .filter(p -> p.getMontoCentavos() == montoCentavos && MONEDA.equals(p.getMoneda()))
            .max(Comparator.comparing(Pago::getId))
            .orElseGet(
                () ->
                    pagoRepository.saveAndFlush(
                        Pago.builder()
                            .reserva(reserva)
                            .referencia(PagoAnticipoService.nuevaReferencia(reservaId))
                            .montoCentavos(montoCentavos)
                            .moneda(MONEDA)
                            .build()));

    PropiedadesPagos.Wompi wompi = propiedadesPagos.wompi();
    String firma =
        FirmasWompi.firmaIntegridad(
            pago.getReferencia(), pago.getMontoCentavos(), MONEDA, wompi.secretoIntegridad());
    String urlRetorno = wompi.urlRetorno().replace("{reservaId}", String.valueOf(reservaId));
    String urlCheckout =
        wompi.urlCheckout()
            + "?public-key="
            + codificar(wompi.llavePublica())
            + "&currency="
            + MONEDA
            + "&amount-in-cents="
            + pago.getMontoCentavos()
            + "&reference="
            + codificar(pago.getReferencia())
            + "&signature:integrity="
            + firma
            + "&redirect-url="
            + codificar(urlRetorno);
    return new CheckoutWompiResponse(
        pago.getId(),
        pago.getReferencia(),
        pago.getMontoCentavos(),
        MONEDA,
        wompi.llavePublica(),
        firma,
        urlRetorno,
        urlCheckout);
  }

  /**
   * Procesa un evento del webhook de Wompi. Solo se acepta si Wompi es el proveedor activo y su
   * checksum es válido; los eventos que no son de transacciones, o de transacciones que no
   * corresponden a un pago, se ignoran sin error para que Wompi no los reintente.
   *
   * @param evento cuerpo del evento tal como lo envía Wompi
   * @throws ProveedorPagosNoDisponibleException si el proveedor activo no es Wompi
   * @throws FirmaWompiInvalidaException si el checksum no coincide
   */
  @Transactional
  public void procesarEvento(JsonNode evento) {
    // CACS-24: con el proveedor simulado el secreto de eventos está vacío y cualquiera podría
    // calcular el checksum; el webhook no se atiende.
    exigirWompi();
    verificarChecksum(evento);
    if (!EVENTO_TRANSACCION.equals(evento.path("event").asString(null))) {
      return;
    }
    Optional<TransaccionWompi> transaccion = transaccionConfiable(evento);
    if (transaccion.isEmpty()) {
      return;
    }
    Optional<Pago> pago = pagoRepository.findByReferencia(transaccion.get().referencia());
    if (pago.isEmpty()) {
      return;
    }
    bloquearReserva(pago.get().getReserva().getId());
    registrar(pago.get(), transaccion.get(), evento.toString());
  }

  /**
   * Consulta en Wompi una transacción de una reserva propia y registra su resultado. Lo usa el
   * frontend al volver del checkout. La consulta a Wompi se hace antes de bloquear la reserva,
   * para no mantener el bloqueo mientras se espera la respuesta.
   *
   * @param reservaId id de la reserva
   * @param emailUsuario correo del usuario autenticado
   * @param idTransaccion id de la transacción que Wompi pone en la URL de retorno
   * @return el pago y el estado de la reserva después de registrarlo
   * @throws ProveedorPagosNoDisponibleException si el proveedor activo no es Wompi
   * @throws ReservaNoEncontradaException si la reserva no existe
   * @throws ReservaAjenaException si la reserva es de otro usuario
   * @throws TransaccionWompiNoEncontradaException si Wompi no conoce la transacción o no es de un
   *     pago de esta reserva
   */
  @Transactional
  public PagoAnticipoResponse consultarTransaccion(
      Long reservaId, String emailUsuario, String idTransaccion) {
    exigirWompi();
    Reserva sinBloquear =
        reservaRepository
            .findById(reservaId)
            .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
    if (!sinBloquear.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
      throw new ReservaAjenaException(reservaId);
    }
    // CACS-24: primero Wompi (puede tardar) y después el bloqueo.
    TransaccionWompi transaccion =
        clienteWompi
            .consultarTransaccion(idTransaccion)
            .orElseThrow(() -> new TransaccionWompiNoEncontradaException(idTransaccion));

    Reserva reserva = bloquearReserva(reservaId);
    Pago pago =
        pagoRepository
            .findByReferencia(transaccion.referencia())
            .filter(p -> p.getReserva().getId().equals(reservaId))
            .orElseThrow(() -> new TransaccionWompiNoEncontradaException(idTransaccion));
    registrar(pago, transaccion, null);

    // El trigger de la base pudo confirmar la reserva: se vuelve a leer.
    entityManager.refresh(reserva);
    return PagoAnticipoResponse.de(pago, reserva);
  }

  /**
   * Guarda el resultado. Si el monto o la moneda no son los del pago, el pago queda en ERROR y la
   * reserva no se confirma. Si la transacción está aprobada pero la reserva ya tiene otro pago
   * aprobado (el cliente pagó dos veces), este pago queda en ERROR para devolverlo a mano: el
   * índice pagos_un_aprobado_por_reserva no admite un segundo pago aprobado.
   */
  private void registrar(Pago pago, TransaccionWompi transaccion, String respuesta) {
    EstadoPago estado =
        transaccion.montoCentavos() == pago.getMontoCentavos()
                && MONEDA.equals(transaccion.moneda())
            ? estadoDe(transaccion.estado())
            : EstadoPago.ERROR;
    if (estado == EstadoPago.APROBADO
        && pagoRepository.existsByReservaIdAndEstadoAndIdNot(
            pago.getReserva().getId(), EstadoPago.APROBADO, pago.getId())) {
      log.warn(
          "Pago {} aprobado en Wompi ({}) para la reserva {}, que ya tenía otro pago aprobado: "
              + "queda en ERROR para devolver el dinero",
          pago.getReferencia(),
          transaccion.id(),
          pago.getReserva().getId());
      estado = EstadoPago.ERROR;
    }
    registroResultadoPago.registrar(
        pago, estado, transaccion.id(), transaccion.metodoPago(), respuesta);
  }

  /**
   * Los datos de la transacción en los que se puede confiar. Si el evento firma la referencia, se
   * usan tal cual; si no, se vuelve a leer la transacción en la API de Wompi por su id (firmado),
   * porque la referencia del cuerpo podría haberse cambiado sin invalidar el checksum.
   */
  private Optional<TransaccionWompi> transaccionConfiable(JsonNode evento) {
    TransaccionWompi delEvento = TransaccionWompi.de(evento.path("data").path("transaction"));
    for (JsonNode propiedad : evento.path("signature").path("properties")) {
      if (PROPIEDAD_REFERENCIA.equals(propiedad.asString())) {
        return Optional.of(delEvento);
      }
    }
    Optional<TransaccionWompi> enWompi = clienteWompi.consultarTransaccion(delEvento.id());
    if (enWompi.isPresent() && !enWompi.get().referencia().equals(delEvento.referencia())) {
      log.warn(
          "Evento de Wompi para la transacción {} con una referencia distinta a la de Wompi: se "
              + "usa la de Wompi",
          delEvento.id());
    }
    return enWompi;
  }

  private void verificarChecksum(JsonNode evento) {
    JsonNode firma = evento.path("signature");
    JsonNode propiedades = firma.path("properties");
    if (!propiedades.isArray() || propiedades.isEmpty() || !evento.path("timestamp").isNumber()) {
      throw new FirmaWompiInvalidaException();
    }
    JsonNode datos = evento.path("data");
    StringBuilder valores = new StringBuilder();
    for (JsonNode propiedad : propiedades) {
      JsonNode valor = datos;
      for (String parte : propiedad.asString().split("\\.")) {
        valor = valor.path(parte);
      }
      if (valor.isMissingNode() || valor.isNull()) {
        throw new FirmaWompiInvalidaException();
      }
      valores.append(valor.asString());
    }
    String esperado =
        FirmasWompi.checksumEvento(
            valores.toString(),
            evento.path("timestamp").asLong(),
            propiedadesPagos.wompi().secretoEventos());
    if (!FirmasWompi.coinciden(esperado, firma.path("checksum").asString(null))) {
      throw new FirmaWompiInvalidaException();
    }
  }

  private void exigirWompi() {
    if (propiedadesPagos.proveedor() != ProveedorPagos.WOMPI) {
      throw new ProveedorPagosNoDisponibleException(propiedadesPagos.proveedor());
    }
  }

  private Reserva bloquearReserva(Long reservaId) {
    return reservaRepository
        .buscarParaPagar(reservaId)
        .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
  }

  /** Estados de transacción de Wompi a estados de pago. */
  static EstadoPago estadoDe(String estadoWompi) {
    return switch (estadoWompi == null ? "" : estadoWompi) {
      case "APPROVED" -> EstadoPago.APROBADO;
      case "DECLINED" -> EstadoPago.RECHAZADO;
      case "VOIDED" -> EstadoPago.ANULADO;
      case "PENDING" -> EstadoPago.PENDIENTE;
      default -> EstadoPago.ERROR;
    };
  }

  private static String codificar(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }
}
