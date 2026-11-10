package com.usta.gamespot.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CACS-27: aplicación consumidora de la arquitectura SOA de Game Spot, en el puerto 8080. Sirve la
 * interfaz web (HTML, CSS y JavaScript) y su backend llama a los proveedores de catálogo (8091) y
 * de reservas (8092) con RestClient.
 */
@SpringBootApplication
public class ConsumidorWebApplication {

  /**
   * Arranca la consumidora.
   *
   * @param args argumentos de la línea de comandos
   */
  public static void main(String[] args) {
    SpringApplication.run(ConsumidorWebApplication.class, args);
  }
}
