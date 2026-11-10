package com.usta.gamespot.reservas.exception;

/** Se lanza cuando se pide una reserva o un usuario que no existe. Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

  /**
   * Crea la excepción.
   *
   * @param recurso nombre del recurso, por ejemplo "la reserva"
   * @param id id buscado
   */
  public RecursoNoEncontradoException(String recurso, Long id) {
    super("No existe " + recurso + " con id " + id);
  }
}
