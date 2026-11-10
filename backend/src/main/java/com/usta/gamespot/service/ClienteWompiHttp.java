package com.usta.gamespot.service;

import com.usta.gamespot.config.PropiedadesPagos;
import com.usta.gamespot.dto.TransaccionWompi;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** CACS-21 (HU-03): cliente HTTP de la API de Wompi. */
@Component
public class ClienteWompiHttp implements ClienteWompi {

  private final RestClient restClient;

  /**
   * Crea el cliente con la URL base de la API configurada.
   *
   * @param propiedades propiedades {@code app.pagos.*}
   */
  public ClienteWompiHttp(PropiedadesPagos propiedades) {
    String urlApi = propiedades.wompi().urlApi();
    this.restClient = RestClient.builder().baseUrl(urlApi == null ? "" : urlApi).build();
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
      throw ex;
    }
  }
}
