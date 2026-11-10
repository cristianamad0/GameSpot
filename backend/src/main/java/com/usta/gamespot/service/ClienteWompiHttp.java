package com.usta.gamespot.service;

import com.usta.gamespot.config.PropiedadesPagos;
import com.usta.gamespot.dto.TransaccionWompi;
import com.usta.gamespot.exception.WompiNoRespondeException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * CACS-21 (HU-03): cliente HTTP de la API de Wompi.
 *
 * <p>CACS-24: tiene un tiempo máximo de conexión y de lectura, para que una respuesta lenta de
 * Wompi no deje esperando la petición indefinidamente; si Wompi no responde o falla, lanza {@link
 * WompiNoRespondeException} (503) en vez de un error genérico.
 */
@Component
public class ClienteWompiHttp implements ClienteWompi {

  /** Tiempo máximo para conectarse con Wompi y para recibir su respuesta. */
  public static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(10);

  private final RestClient restClient;

  /**
   * Crea el cliente con la URL base de la API configurada.
   *
   * @param propiedades propiedades {@code app.pagos.*}
   */
  @Autowired
  public ClienteWompiHttp(PropiedadesPagos propiedades) {
    this(propiedades.wompi().urlApi(), TIEMPO_MAXIMO);
  }

  /**
   * Crea el cliente con una URL y un tiempo máximo dados. Lo usan las pruebas.
   *
   * @param urlApi URL base de la API de Wompi
   * @param tiempoMaximo tiempo máximo de conexión y de lectura
   */
  public ClienteWompiHttp(String urlApi, Duration tiempoMaximo) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(tiempoMaximo);
    fabrica.setReadTimeout(tiempoMaximo);
    this.restClient =
        RestClient.builder()
            .baseUrl(urlApi == null ? "" : urlApi)
            .requestFactory(fabrica)
            .build();
  }

  @Override
  public Optional<TransaccionWompi> consultarTransaccion(String idTransaccion) {
    try {
      JsonNode respuesta =
          restClient.get().uri("/transactions/{id}", idTransaccion).retrieve().body(JsonNode.class);
      if (respuesta == null || respuesta.path("data").isMissingNode()) {
        return Optional.empty();
      }
      return Optional.of(TransaccionWompi.de(respuesta.path("data")));
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw new WompiNoRespondeException(ex);
    } catch (RestClientException ex) {
      // 5xx, tiempo agotado o error de red.
      throw new WompiNoRespondeException(ex);
    }
  }
}
