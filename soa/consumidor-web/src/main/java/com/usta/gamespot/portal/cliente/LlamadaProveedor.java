package com.usta.gamespot.portal.cliente;

import com.usta.gamespot.portal.dto.ApiError;
import com.usta.gamespot.portal.exception.ErrorProveedorException;
import com.usta.gamespot.portal.exception.ServicioNoDisponibleException;
import java.util.function.Supplier;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envuelve cada llamada HTTP a un proveedor y traduce sus fallos:
 *
 * <ul>
 *   <li>Sin conexión o tiempo agotado: {@link ServicioNoDisponibleException} (503).
 *   <li>Respuesta de error del proveedor: {@link ErrorProveedorException} con su código y mensaje.
 * </ul>
 */
final class LlamadaProveedor {

  private LlamadaProveedor() {}

  static <T> T ejecutar(String servicio, Supplier<T> llamada) {
    try {
      return llamada.get();
    } catch (ResourceAccessException ex) {
      throw new ServicioNoDisponibleException(servicio, ex);
    } catch (RestClientResponseException ex) {
      ApiError error = leerError(ex);
      String mensaje =
          error != null && error.mensaje() != null
              ? error.mensaje()
              : "El servicio de " + servicio + " respondió con el error " + ex.getStatusCode().value();
      throw new ErrorProveedorException(
          ex.getStatusCode().value(), mensaje, error == null ? null : error.detalles());
    }
  }

  static void ejecutar(String servicio, Runnable llamada) {
    ejecutar(
        servicio,
        () -> {
          llamada.run();
          return null;
        });
  }

  private static ApiError leerError(RestClientResponseException ex) {
    try {
      return ex.getResponseBodyAs(ApiError.class);
    } catch (RuntimeException noEsJson) {
      return null;
    }
  }
}
