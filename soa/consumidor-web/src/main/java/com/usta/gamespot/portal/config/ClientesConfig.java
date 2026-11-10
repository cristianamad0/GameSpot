package com.usta.gamespot.portal.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * CACS-27: un RestClient por proveedor, con su URL base y tiempos de espera configurables. Si un
 * proveedor está detenido, la conexión falla en pocos segundos en vez de dejar la página colgada.
 */
@Configuration
public class ClientesConfig {

  @Value("${app.proveedores.tiempo-conexion-ms}")
  private long tiempoConexionMs;

  @Value("${app.proveedores.tiempo-lectura-ms}")
  private long tiempoLecturaMs;

  /**
   * Cliente del proveedor de catálogo.
   *
   * @param url URL base del proveedor (app.proveedores.catalogo.url)
   * @return el RestClient
   */
  @Bean
  public RestClient clienteCatalogo(@Value("${app.proveedores.catalogo.url}") String url) {
    return crear(url);
  }

  /**
   * Cliente del proveedor de reservas.
   *
   * @param url URL base del proveedor (app.proveedores.reservas.url)
   * @return el RestClient
   */
  @Bean
  public RestClient clienteReservas(@Value("${app.proveedores.reservas.url}") String url) {
    return crear(url);
  }

  private RestClient crear(String url) {
    HttpClient http =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(tiempoConexionMs)).build();
    JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
    fabrica.setReadTimeout(Duration.ofMillis(tiempoLecturaMs));
    return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
  }
}
