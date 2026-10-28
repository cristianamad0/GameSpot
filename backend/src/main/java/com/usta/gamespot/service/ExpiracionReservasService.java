package com.usta.gamespot.service;

import com.usta.gamespot.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CACS-12 (HU-05): libera las reservas que no se pagaron dentro del plazo. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiracionReservasService {

  private final ReservaRepository reservaRepository;

  /**
   * Pasa a EXPIRADA toda reserva en PENDIENTE_PAGO cuyo plazo de pago ya venció. Así su bloque
   * queda libre para otra reserva. Las reservas en cualquier otro estado no cambian.
   *
   * @return cuántas reservas expiraron
   */
  @Transactional
  public int expirarVencidas() {
    int expiradas = reservaRepository.expirarPendientes();
    if (expiradas > 0) {
      log.info("Reservas expiradas por falta de pago: {}", expiradas);
    } else {
      log.debug("Reservas expiradas por falta de pago: 0");
    }
    return expiradas;
  }
}
