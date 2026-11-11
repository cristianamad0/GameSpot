package com.usta.gamespot.catalogo.exception;

import com.usta.gamespot.catalogo.dto.ApiError;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Convierte las excepciones en respuestas {@link ApiError} con el código HTTP adecuado. */
@RestControllerAdvice
public class ManejadorErrores {

  /**
   * Recurso inexistente.
   *
   * @param ex excepción con el recurso y el id
   * @return 404
   */
  @ExceptionHandler(RecursoNoEncontradoException.class)
  public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex) {
    return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
  }

  /**
   * Conflicto con el estado actual.
   *
   * @param ex excepción con la explicación
   * @return 409
   */
  @ExceptionHandler(ConflictoException.class)
  public ResponseEntity<ApiError> conflicto(ConflictoException ex) {
    return respuesta(HttpStatus.CONFLICT, ex.getMessage(), List.of());
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
    return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
  }

  /**
   * Cuerpo JSON mal formado o parámetro con un tipo incorrecto.
   *
   * @param ex excepción de lectura o de conversión
   * @return 400
   */
  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> malFormado(Exception ex) {
    return respuesta(HttpStatus.BAD_REQUEST, "La solicitud no tiene el formato esperado", List.of());
  }

  private static ResponseEntity<ApiError> respuesta(
      HttpStatus estado, String mensaje, List<String> detalles) {
    return ResponseEntity.status(estado).body(ApiError.de(estado.value(), mensaje, detalles));
  }
}
