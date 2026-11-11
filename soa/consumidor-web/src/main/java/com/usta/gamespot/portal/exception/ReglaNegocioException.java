package com.usta.gamespot.portal.exception;

import lombok.Getter;

/**
 * Una regla que valida la propia consumidora al coordinar los dos proveedores (por ejemplo, una
 * reserva fuera del horario de atención o sobre un equipo fuera de servicio).
 */
@Getter
public class ReglaNegocioException extends RuntimeException {

  private final int status;

  /**
   * Crea la excepción.
   *
   * @param status 400 si los datos no sirven; 409 si el estado del equipo no lo permite
   * @param mensaje explicación para el usuario
   */
  public ReglaNegocioException(int status, String mensaje) {
    super(mensaje);
    this.status = status;
  }
}
