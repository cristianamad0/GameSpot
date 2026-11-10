package com.usta.gamespot.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CACS-26: Proveedor 2 de la arquitectura SOA de Game Spot. Gestiona las reservas por bloques de 30
 * minutos y emite los JWT de los usuarios, en el puerto 8092.
 */
@SpringBootApplication
public class ProveedorReservasApplication {

  /**
   * Arranca el proveedor de reservas.
   *
   * @param args argumentos de la línea de comandos
   */
  public static void main(String[] args) {
    SpringApplication.run(ProveedorReservasApplication.class, args);
  }
}
