package com.usta.gamespot.portal.exception;

import com.usta.gamespot.portal.dto.ApiError;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Convierte los errores de la consumidora y de los proveedores en respuestas {@link ApiError}. */
@RestControllerAdvice
public class ManejadorErrores {

  /**
   * Proveedor detenido o que no responde.
   *
   * @param ex excepción con el nombre del servicio
   * @return 503
   */
  @ExceptionHandler(ServicioNoDisponibleException.class)
  public ResponseEntity<ApiError> noDisponible(ServicioNoDisponibleException ex) {
    return respuesta(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage(), List.of());
  }

  /**
   * Error HTTP de un proveedor: se entrega con su mismo código y mensaje.
   *
   * @param ex excepción con el código y el mensaje del proveedor
   * @return el mismo código del proveedor
   */
  @ExceptionHandler(ErrorProveedorException.class)
  public ResponseEntity<ApiError> errorProveedor(ErrorProveedorException ex) {
    return respuesta(ex.getStatus(), ex.getMessage(), ex.getDetalles());
  }

  /**
   * Regla validada por la consumidora.
   *
   * @param ex excepción con el código y la explicación
   * @return 400 o 409
   */
  @ExceptionHandler(ReglaNegocioException.class)
  public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex) {
    return respuesta(ex.getStatus(), ex.getMessage(), List.of());
  }

  /**
   * Campos que no pasan la validación.
   *
   * @param ex excepción con los errores de cada campo
   * @return 400 con un detalle por campo
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
    List<String> detalles =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
    return respuesta(400, "Datos inválidos", detalles);
  }

  /**
   * Cuerpo o parámetros con un formato incorrecto.
   *
   * @param ex excepción de lectura o de conversión
   * @return 400
   */
  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiError> malFormado(Exception ex) {
    return respuesta(400, "La solicitud no tiene el formato esperado", List.of());
  }

  /**
   * Operación protegida sin el encabezado Authorization.
   *
   * @param ex excepción del encabezado faltante
   * @return 401
   */
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiError> sinToken(MissingRequestHeaderException ex) {
    return respuesta(401, "Inicia sesión para hacer esta operación", List.of());
  }

  private static ResponseEntity<ApiError> respuesta(
      int estado, String mensaje, List<String> detalles) {
    return ResponseEntity.status(estado).body(ApiError.de(estado, mensaje, detalles));
  }
}
