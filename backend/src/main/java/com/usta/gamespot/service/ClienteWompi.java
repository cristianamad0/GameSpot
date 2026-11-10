package com.usta.gamespot.service;

import com.usta.gamespot.dto.TransaccionWompi;
import java.util.Optional;

/**
 * CACS-21 (HU-03): consultas a la API de Wompi. En las pruebas automatizadas se reemplaza por un
 * Wompi simulado: ninguna prueba llama a Wompi.
 */
public interface ClienteWompi {

  /**
   * Consulta una transacción por su id ({@code GET /transactions/{id}}). No necesita la llave
   * privada.
   *
   * @param idTransaccion id que Wompi devuelve al terminar el checkout
   * @return la transacción, o vacío si Wompi no la conoce
   */
  Optional<TransaccionWompi> consultarTransaccion(String idTransaccion);
}
