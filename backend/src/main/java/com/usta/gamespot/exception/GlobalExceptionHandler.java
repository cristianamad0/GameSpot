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
   * Responde a una reserva que no existe.
   *
   * @param ex excepción con el id buscado
   * @return 404 con el mensaje de la excepción
   */
  @ExceptionHandler(ReservaNoEncontradaException.class)
  public ResponseEntity<ApiError> manejarReservaNoEncontrada(ReservaNoEncontradaException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.de(404, ex.getMessage(), List.of()));
  }

  /**
   * Responde a un cambio de estado que el ciclo de vida de la reserva no permite.
   *
   * @param ex excepción con el estado actual y la acción intentada
   * @return 409 con el mensaje de la excepción
   */
  @ExceptionHandler(TransicionReservaInvalidaException.class)
  public ResponseEntity<ApiError> manejarTransicionInvalida(
      TransicionReservaInvalidaException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.de(409, ex.getMessage(), List.of()));
  }

  /**
   * Responde a un intento de pagar la reserva de otro usuario.
   *
   * @param ex excepción con el id de la reserva
   * @return 403 con el mensaje de la excepción
   */
  @ExceptionHandler(ReservaAjenaException.class)
  public ResponseEntity<ApiError> manejarReservaAjena(ReservaAjenaException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiError.de(403, ex.getMessage(), List.of()));
  }

  /**
   * Responde a un intento de pagar una reserva cuyo plazo de pago ya venció.
   *
   * @param ex excepción con el id de la reserva
   * @return 409 con el mensaje de la excepción
   */
  @ExceptionHandler(PlazoPagoVencidoException.class)
  public ResponseEntity<ApiError> manejarPlazoPagoVencido(PlazoPagoVencidoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.de(409, ex.getMessage(), List.of()));
  }

  /**
   * Responde a un usuario que no existe.
   *
   * @param ex excepción con el id buscado
   * @return 404 con el mensaje de la excepción
   */
  @ExceptionHandler(UsuarioNoEncontradoException.class)
  public ResponseEntity<ApiError> manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.de(404, ex.getMessage(), List.of()));
  }

  /**
   * Responde a un ajuste de puntos que dejaría el saldo en negativo.
   *
   * @param ex excepción con el saldo actual y el ajuste pedido
   * @return 409 con el mensaje de la excepción
   */
  @ExceptionHandler(SaldoPuntosInsuficienteException.class)
  public ResponseEntity<ApiError> manejarSaldoInsuficiente(SaldoPuntosInsuficienteException ex) {
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
