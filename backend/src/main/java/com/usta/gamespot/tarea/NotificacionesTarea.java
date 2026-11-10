package com.usta.gamespot.tarea;

import com.usta.gamespot.service.NotificacionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CACS-8 (HU-08): envía las notificaciones pendientes cada {@code
 * app.notificaciones.envio.intervalo-ms} milisegundos. Solo corre con {@code
 * app.notificaciones.envio.habilitada=true}, porque necesita las credenciales de correo.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notificaciones.envio.habilitada", havingValue = "true")
public class NotificacionesTarea {

  private final NotificacionService notificacionService;

  /** Cuenta el intervalo desde que termina la ejecución anterior, para que no se solapen. */
  @Scheduled(fixedDelayString = "${app.notificaciones.envio.intervalo-ms}")
  public void ejecutar() {
    notificacionService.enviarPendientes(Instant.now());
  }
}
