package com.usta.gamespot.exception;

import com.usta.gamespot.dto.ApiError;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Convierte las excepciones de la API en respuestas {@link ApiError}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Responde al correo ya registrado.
   *
   * @param ex excepción lanzada en el registro
   * @return 409 con el mensaje de la excepción
   */
  @ExceptionHandler(EmailYaRegistradoException.class)
  public ResponseEntity<ApiError> manejarEmailDuplicado(EmailYaRegistradoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.de(409, ex.getMessage(), List.of()));
  }

  /**
   * Responde a credenciales incorrectas en el login.
   *
   * @param ex excepción lanzada por Spring Security
   * @return 401 con un mensaje genérico
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> manejarCredencialesInvalidas(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiError.de(401, "Correo o contraseña incorrectos", List.of()));
  }

  /**
   * Responde a datos de entrada que no pasan la validación.
   *
   * @param ex excepción con los errores de cada campo
   * @return 400 con un detalle por campo inválido
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex) {
    List<String> detalles =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.de(400, "Datos invalidos", detalles));
  }
}
