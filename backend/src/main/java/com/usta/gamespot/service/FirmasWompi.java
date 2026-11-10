package com.usta.gamespot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * CACS-21 (HU-03): firmas SHA-256 que exige Wompi.
 *
 * <ul>
 *   <li>Integridad del checkout: SHA-256 de referencia + monto en centavos + moneda + secreto de
 *       integridad. Impide que el cliente cambie el monto o la referencia en el navegador.
 *   <li>Checksum de un evento: SHA-256 de los valores de {@code signature.properties}, en orden,
 *       + {@code timestamp} + secreto de eventos. Prueba que el evento lo envió Wompi.
 * </ul>
 */
public final class FirmasWompi {

  private FirmasWompi() {}

  /**
   * Firma de integridad para el Web Checkout.
   *
   * @param referencia referencia única del pago
   * @param montoCentavos monto en centavos
   * @param moneda moneda (COP)
   * @param secretoIntegridad secreto de integridad del comercio
   * @return la firma en hexadecimal (minúsculas)
   */
  public static String firmaIntegridad(
      String referencia, long montoCentavos, String moneda, String secretoIntegridad) {
    return sha256(referencia + montoCentavos + moneda + secretoIntegridad);
  }

  /**
   * Checksum esperado de un evento.
   *
   * @param valoresConcatenados valores de {@code signature.properties}, ya concatenados en orden
   * @param timestamp {@code timestamp} del evento
   * @param secretoEventos secreto de eventos del comercio
   * @return el checksum en hexadecimal (minúsculas)
   */
  public static String checksumEvento(
      String valoresConcatenados, long timestamp, String secretoEventos) {
    return sha256(valoresConcatenados + timestamp + secretoEventos);
  }

  /**
   * Compara dos firmas en tiempo constante, sin distinguir mayúsculas.
   *
   * @param esperada firma calculada por el sistema
   * @param recibida firma que llegó en el evento; puede ser null
   * @return true si coinciden
   */
  public static boolean coinciden(String esperada, String recibida) {
    if (recibida == null) {
      return false;
    }
    return MessageDigest.isEqual(
        esperada.toLowerCase().getBytes(StandardCharsets.UTF_8),
        recibida.toLowerCase().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(String texto) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("La JVM no tiene SHA-256", ex);
    }
  }
}
