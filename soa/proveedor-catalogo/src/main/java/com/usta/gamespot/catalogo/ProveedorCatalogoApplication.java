package com.usta.gamespot.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CACS-25: Proveedor 1 de la arquitectura SOA de Game Spot. Expone el catálogo de equipos,
 * modalidades (con su tarifa por hora) y horario de atención en el puerto 8091.
 */
@SpringBootApplication
public class ProveedorCatalogoApplication {

  /**
   * Arranca el proveedor de catálogo.
   *
   * @param args argumentos de la línea de comandos
   */
  public static void main(String[] args) {
    SpringApplication.run(ProveedorCatalogoApplication.class, args);
  }
}
