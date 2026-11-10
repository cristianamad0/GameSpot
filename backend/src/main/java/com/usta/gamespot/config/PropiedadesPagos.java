package com.usta.gamespot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CACS-21 (HU-03): propiedades {@code app.pagos.*}. Las llaves y secretos de Wompi llegan por
 * variables de entorno ({@code WOMPI_*}); nunca se escriben en archivos versionados.
 *
 * <p>Si el proveedor es WOMPI, la aplicación no arranca sin las llaves: así no queda funcionando
 * con un proveedor a medio configurar.
 *
 * @param proveedor proveedor activo; SIMULADO si no se indica
 * @param wompi datos de conexión con Wompi
 */
@ConfigurationProperties(prefix = "app.pagos")
public record PropiedadesPagos(ProveedorPagos proveedor, Wompi wompi) {

  /**
   * Valida las propiedades al arrancar.
   *
   * @param proveedor proveedor activo
   * @param wompi datos de conexión con Wompi
   */
  public PropiedadesPagos {
    if (proveedor == null) {
      proveedor = ProveedorPagos.SIMULADO;
    }
    if (wompi == null) {
      wompi = new Wompi(null, null, null, null, null, null);
    }
    if (proveedor == ProveedorPagos.WOMPI && !wompi.estaCompleta()) {
      throw new IllegalStateException(
          "app.pagos.proveedor=wompi necesita WOMPI_LLAVE_PUBLICA, WOMPI_SECRETO_EVENTOS,"
              + " WOMPI_SECRETO_INTEGRIDAD y las URL de la API, del checkout y de retorno");
    }
  }

  /**
   * Datos de conexión con Wompi.
   *
   * @param llavePublica llave pública ({@code pub_test_...} en Sandbox)
   * @param secretoEventos secreto para verificar el checksum de los eventos
   * @param secretoIntegridad secreto para firmar el monto y la referencia del checkout
   * @param urlApi URL base de la API, por ejemplo {@code https://sandbox.wompi.co/v1}
   * @param urlCheckout URL del Web Checkout, {@code https://checkout.wompi.co/p/}
   * @param urlRetorno adonde vuelve el cliente al pagar; {@code {reservaId}} se reemplaza por el
   *     id de la reserva
   */
  public record Wompi(
      String llavePublica,
      String secretoEventos,
      String secretoIntegridad,
      String urlApi,
      String urlCheckout,
      String urlRetorno) {

    /**
     * Indica si están todos los datos para usar Wompi.
     *
     * @return true si ninguno está vacío
     */
    public boolean estaCompleta() {
      return tieneTexto(llavePublica)
          && tieneTexto(secretoEventos)
          && tieneTexto(secretoIntegridad)
          && tieneTexto(urlApi)
          && tieneTexto(urlCheckout)
          && tieneTexto(urlRetorno);
    }

    private static boolean tieneTexto(String valor) {
      return valor != null && !valor.isBlank();
    }
  }
}
