package com.usta.gamespot.portal.exception;

/**
 * Un proveedor no respondió (está detenido, no existe la URL o tardó demasiado). La consumidora
 * sigue funcionando y responde 503 con el nombre del servicio que falló.
 */
public class ServicioNoDisponibleException extends RuntimeException {

  /**
   * Crea la excepción.
   *
   * @param servicio nombre del servicio, por ejemplo "catálogo"
   * @param causa error de conexión original
   */
  public ServicioNoDisponibleException(String servicio, Throwable causa) {
    super(
        "El servicio de "
            + servicio
            + " no está disponible en este momento. Intenta de nuevo en unos minutos.",
        causa);
  }
}
