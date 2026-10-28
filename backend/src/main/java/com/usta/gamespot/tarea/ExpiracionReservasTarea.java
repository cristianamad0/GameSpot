package com.usta.gamespot.tarea;

import com.usta.gamespot.service.ExpiracionReservasService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta la expiración de reservas no pagadas cada {@code app.reservas.expiracion.intervalo-ms}
 * milisegundos. Se apaga con {@code app.reservas.expiracion.habilitada=false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reservas.expiracion.habilitada", havingValue = "true")
public class ExpiracionReservasTarea {

  private final ExpiracionReservasService expiracionReservasService;

  /** Cuenta el intervalo desde que termina la ejecución anterior, para que no se solapen. */
  @Scheduled(fixedDelayString = "${app.reservas.expiracion.intervalo-ms}")
  public void ejecutar() {
    expiracionReservasService.expirarVencidas();
  }
}
