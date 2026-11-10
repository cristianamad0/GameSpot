package com.usta.gamespot.service;

/**
 * CACS-8 (HU-08): envía un correo de texto. Lo implementa {@link EnviadorCorreoSmtp} con Gmail; en
 * las pruebas automatizadas se reemplaza por uno simulado, así que ninguna prueba envía correos.
 */
public interface EnviadorCorreo {

  /**
   * Envía el correo.
   *
   * @param para correo del destinatario
   * @param asunto asunto del mensaje
   * @param cuerpo texto del mensaje
   * @throws RuntimeException si el servidor de correo rechaza el envío o no responde
   */
  void enviar(String para, String asunto, String cuerpo);
}
