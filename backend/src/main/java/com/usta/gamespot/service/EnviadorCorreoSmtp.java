package com.usta.gamespot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * CACS-8 (HU-08): envía los correos con Gmail por SMTP (spring.mail.*). El usuario y la contraseña
 * de aplicación llegan por las variables CORREO_USUARIO y CORREO_CONTRASENA_APLICACION; el
 * remitente es esa misma cuenta, como exige Gmail.
 */
@Component
@RequiredArgsConstructor
public class EnviadorCorreoSmtp implements EnviadorCorreo {

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username:}")
  private String remitente;

  @Override
  public void enviar(String para, String asunto, String cuerpo) {
    SimpleMailMessage mensaje = new SimpleMailMessage();
    mensaje.setFrom("Game Spot <" + remitente + ">");
    mensaje.setTo(para);
    mensaje.setSubject(asunto);
    mensaje.setText(cuerpo);
    javaMailSender.send(mensaje);
  }
}
