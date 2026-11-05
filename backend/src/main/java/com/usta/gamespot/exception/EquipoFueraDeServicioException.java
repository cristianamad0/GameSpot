package com.usta.gamespot.exception;

/**
 * CACS-20: se lanza al reservar un equipo fuera de servicio. Los datos de la reserva son válidos;
 * lo que lo impide es el estado del equipo, por eso responde 409 y no 400.
 */
public class EquipoFueraDeServicioException extends RuntimeException {
  /**
   * Crea la excepción con el nombre del equipo en el mensaje.
   *
   * @param nombreEquipo nombre del equipo fuera de servicio
   */
  public EquipoFueraDeServicioException(String nombreEquipo) {
    super("El equipo " + nombreEquipo + " está fuera de servicio");
  }
}
