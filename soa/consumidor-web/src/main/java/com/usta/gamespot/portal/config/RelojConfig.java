package com.usta.gamespot.portal.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj del sistema; las pruebas lo reemplazan por uno fijo para no depender de la hora real. */
@Configuration
public class RelojConfig {

  /**
   * Reloj del sistema en UTC.
   *
   * @return el reloj
   */
  @Bean
  public Clock reloj() {
    return Clock.systemUTC();
  }
}
