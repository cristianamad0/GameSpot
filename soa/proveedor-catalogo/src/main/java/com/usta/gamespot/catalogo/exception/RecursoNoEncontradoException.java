package com.usta.gamespot.catalogo.exception;

/** Se lanza cuando se pide un equipo o una modalidad que no existe. Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

  /**
   * Crea la excepción.
   *
   * @param recurso nombre del recurso, por ejemplo "el equipo"
   * @param id id buscado
   */
  public RecursoNoEncontradoException(String recurso, Long id) {
    super("No existe " + recurso + " con id " + id);
  }
}
