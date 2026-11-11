package com.usta.gamespot.portal.exception;

import java.util.List;
import lombok.Getter;

/**
 * Un proveedor respondió con un error HTTP (400, 401, 403, 404, 409...). La consumidora lo
 * entrega al navegador con el mismo código y el mismo mensaje.
 */
@Getter
public class ErrorProveedorException extends RuntimeException {

  private final int status;
  private final List<String> detalles;

  /**
   * Crea la excepción.
   *
   * @param status código HTTP del proveedor
   * @param mensaje mensaje del proveedor
   * @param detalles detalles del proveedor
   */
  public ErrorProveedorException(int status, String mensaje, List<String> detalles) {
    super(mensaje);
    this.status = status;
    this.detalles = detalles == null ? List.of() : detalles;
  }
}
