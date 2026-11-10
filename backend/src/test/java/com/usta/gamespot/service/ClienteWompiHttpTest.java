package com.usta.gamespot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.usta.gamespot.dto.TransaccionWompi;
import com.usta.gamespot.exception.WompiNoRespondeException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CACS-24 (HU-03): el cliente HTTP de Wompi contra un servidor local que imita a Wompi. No usa
 * Spring ni Docker y no sale a internet.
 */
class ClienteWompiHttpTest {

  private static final Duration TIEMPO_CORTO = Duration.ofMillis(300);

  private HttpServer servidor;
  private ClienteWompiHttp cliente;

  @BeforeEach
  void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    servidor.createContext(
        "/transactions/tx-aprobada",
        intercambio ->
            responder(
                intercambio,
                200,
                """
                {"data": {"id": "tx-aprobada", "reference": "GS-1-abc", "status": "APPROVED",
                  "amount_in_cents": 250000, "currency": "COP", "payment_method_type": "NEQUI"}}
                """));
    servidor.createContext(
        "/transactions/tx-inexistente", intercambio -> responder(intercambio, 404, "{}"));
    servidor.createContext(
        "/transactions/tx-falla", intercambio -> responder(intercambio, 502, "{}"));
    servidor.createContext(
        "/transactions/tx-lenta",
        intercambio -> {
          try {
            Thread.sleep(TIEMPO_CORTO.multipliedBy(5).toMillis());
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
          responder(intercambio, 200, "{}");
        });
    servidor.start();
    cliente =
        new ClienteWompiHttp(
            "http://127.0.0.1:" + servidor.getAddress().getPort(), TIEMPO_CORTO);
  }

  @AfterEach
  void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int estado, String cuerpo)
      throws IOException {
    byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
    intercambio.getResponseHeaders().add("Content-Type", "application/json");
    intercambio.sendResponseHeaders(estado, bytes.length);
    try (var salida = intercambio.getResponseBody()) {
      salida.write(bytes);
    }
  }

  @Test
  @DisplayName("CP-36g Lee la transacción que Wompi conoce y da vacío si responde 404")
  void leeLaTransaccionOVacio() {
    Optional<TransaccionWompi> aprobada = cliente.consultarTransaccion("tx-aprobada");

    assertTrue(aprobada.isPresent());
    assertEquals("GS-1-abc", aprobada.get().referencia());
    assertEquals(250_000L, aprobada.get().montoCentavos());
    assertEquals("NEQUI", aprobada.get().metodoPago());
    assertTrue(cliente.consultarTransaccion("tx-inexistente").isEmpty());
  }

  @Test
  @DisplayName("CP-36h Si Wompi falla (5xx) o no responde a tiempo, lanza WompiNoRespondeException")
  void fallaOLentitudDeWompi() {
    assertThrows(WompiNoRespondeException.class, () -> cliente.consultarTransaccion("tx-falla"));

    long antes = System.nanoTime();
    assertThrows(WompiNoRespondeException.class, () -> cliente.consultarTransaccion("tx-lenta"));
    Duration espera = Duration.ofNanos(System.nanoTime() - antes);
    assertTrue(
        espera.compareTo(TIEMPO_CORTO.multipliedBy(4)) < 0,
        "Esperó " + espera.toMillis() + " ms: no respetó el tiempo máximo");
  }
}
