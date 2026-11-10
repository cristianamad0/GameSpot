package com.usta.gamespot.service;

import static com.usta.gamespot.service.AdminReservasService.ZONA_NEGOCIO;

import com.usta.gamespot.model.Notificacion;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.TipoNotificacion;
import com.usta.gamespot.repository.NotificacionRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-8 (HU-08): notificaciones por correo de las reservas confirmadas.
 *
 * <p>Al confirmarse una reserva se programan la CONFIRMACION, para enviarse de inmediato, y el
 * RECORDATORIO, una hora antes del inicio. Las crea {@link RegistroResultadoPago}, el único punto
 * por donde pasa todo pago aprobado, así que salen igual con el proveedor simulado y con Wompi.
 * La tarea NotificacionesTarea las envía; si un envío falla, se reintenta en la siguiente
 * ejecución.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

  /** Cuánto antes del inicio se envía el recordatorio. */
  public static final Duration ANTICIPACION_RECORDATORIO = Duration.ofHours(1);

  /** Cuántas notificaciones se envían como máximo en cada ejecución de la tarea. */
  private static final int LOTE = 50;

  /** ultimo_error es text, pero un mensaje corto basta para saber qué pasó. */
  private static final int LARGO_MAXIMO_ERROR = 500;

  private static final Locale ESPANOL_COLOMBIA = Locale.of("es", "CO");
  private static final DateTimeFormatter FECHA_HORA =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' h:mm a", ESPANOL_COLOMBIA)
          .withZone(ZONA_NEGOCIO);

  private final NotificacionRepository notificacionRepository;
  private final EnviadorCorreo enviadorCorreo;

  /**
   * Programa las notificaciones de una reserva recién confirmada. Si faltaba menos de una hora
   * para el inicio, solo programa la confirmación. Si la reserva ya tenía su confirmación, no
   * hace nada, así que se puede llamar más de una vez.
   *
   * @param reserva reserva en estado CONFIRMADA
   * @param ahora momento de la confirmación
   */
  @Transactional
  public void programarAlConfirmar(Reserva reserva, Instant ahora) {
    if (notificacionRepository.existsByReservaIdAndTipo(
        reserva.getId(), TipoNotificacion.CONFIRMACION)) {
      return;
    }
    programar(reserva, TipoNotificacion.CONFIRMACION, ahora);
    Instant recordatorio = reserva.getInicio().minus(ANTICIPACION_RECORDATORIO);
    if (recordatorio.isAfter(ahora)) {
      programar(reserva, TipoNotificacion.RECORDATORIO, recordatorio);
    }
  }

  /**
   * Envía las notificaciones cuya hora ya llegó y que no se han enviado. Las enviadas quedan con
   * fueEnviada y enviadaEn; las que fallan suman un intento, guardan el error y se reintentan en
   * la siguiente ejecución. El recordatorio de una reserva que ya no está CONFIRMADA no se envía.
   *
   * @param ahora momento de la ejecución
   * @return cuántas notificaciones se enviaron
   */
  @Transactional
  public int enviarPendientes(Instant ahora) {
    List<Notificacion> pendientes =
        notificacionRepository.buscarPorEnviar(ahora, PageRequest.of(0, LOTE));
    int enviadas = 0;
    for (Notificacion notificacion : pendientes) {
      try {
        enviadorCorreo.enviar(
            notificacion.getUsuario().getEmail(), asunto(notificacion), cuerpo(notificacion));
        notificacion.setFueEnviada(true);
        notificacion.setEnviadaEn(Instant.now());
        enviadas++;
      } catch (RuntimeException ex) {
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        notificacion.setUltimoError(recortar(ex.getMessage()));
        log.warn(
            "No se pudo enviar la notificación {} (intento {}): {}",
            notificacion.getId(),
            notificacion.getIntentos(),
            ex.getMessage());
      }
    }
    notificacionRepository.saveAllAndFlush(pendientes);
    if (!pendientes.isEmpty()) {
      log.info("Notificaciones enviadas: {} de {}", enviadas, pendientes.size());
    }
    return enviadas;
  }

  private void programar(Reserva reserva, TipoNotificacion tipo, Instant programadaPara) {
    notificacionRepository.save(
        Notificacion.builder()
            .usuario(reserva.getUsuario())
            .reserva(reserva)
            .tipo(tipo)
            .programadaPara(programadaPara)
            .build());
  }

  private static String asunto(Notificacion notificacion) {
    return notificacion.getTipo() == TipoNotificacion.CONFIRMACION
        ? "Tu reserva en Game Spot está confirmada"
        : "Tu turno en Game Spot empieza en 1 hora";
  }

  private static String cuerpo(Notificacion notificacion) {
    Reserva reserva = notificacion.getReserva();
    String fecha = FECHA_HORA.format(reserva.getInicio());
    long duracionMinutos = Duration.between(reserva.getInicio(), reserva.getFin()).toMinutes();
    String saludo = "Hola, " + notificacion.getUsuario().getNombre() + ".\n\n";
    String detalle =
        "Equipo: "
            + reserva.getEquipo().getNombre()
            + "\nFecha: "
            + fecha
            + "\nDuración: "
            + duracionMinutos
            + " minutos\nAdelanto pagado: "
            + pesos(reserva.getAdelantoPesos())
            + "\nSaldo a pagar en el local: "
            + pesos(reserva.getPrecioTotalPesos() - reserva.getAdelantoPesos())
            + "\n\n";
    String despedida = "Game Spot · Sala de videojuegos · Villavicencio\n";
    if (notificacion.getTipo() == TipoNotificacion.CONFIRMACION) {
      return saludo
          + "Tu reserva quedó confirmada.\n\n"
          + detalle
          + "Te esperamos. Si no puedes asistir, avísale al local.\n\n"
          + despedida;
    }
    return saludo
        + "Te recordamos que tu turno empieza en 1 hora.\n\n"
        + detalle
        + "Llega unos minutos antes para empezar a tiempo.\n\n"
        + despedida;
  }

  /** "$2.500": pesos sin decimales y con punto de miles, como en Colombia. */
  private static String pesos(int valor) {
    return "$" + String.format(ESPANOL_COLOMBIA, "%,d", valor);
  }

  private static String recortar(String mensaje) {
    String texto = mensaje == null ? "Error sin mensaje" : mensaje;
    return texto.length() <= LARGO_MAXIMO_ERROR ? texto : texto.substring(0, LARGO_MAXIMO_ERROR);
  }
}
