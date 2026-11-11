package com.usta.gamespot.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.usta.gamespot.portal.cliente.CatalogoCliente;
import com.usta.gamespot.portal.cliente.ReservasCliente;
import com.usta.gamespot.portal.dto.CrearReservaDto;
import com.usta.gamespot.portal.exception.ErrorProveedorException;
import com.usta.gamespot.portal.exception.ServicioNoDisponibleException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Pruebas de los clientes HTTP de la consumidora (CACS-27): rutas y encabezados que envía el
 * RestClient, traducción de los errores de un proveedor y comportamiento con un proveedor detenido.
 * No necesitan Spring ni los proveedores reales.
 */
class ClientesHttpTests {

  @Test
  @DisplayName("CP-38g RestClient pide GET /api/equipos al catálogo y lee el JSON")
  void catalogoLeeEquipos() {
    RestClient.Builder constructor = RestClient.builder().baseUrl("http://catalogo");
    MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
    servidor
        .expect(requestTo("http://catalogo/api/equipos"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "[{\"id\":1,\"nombre\":\"Xbox 360 #1\",\"modalidadId\":1,\"modalidad\":\"Xbox 360\","
                    + "\"tarifaHoraPesos\":5000,\"tarifaMediaHoraPesos\":3000,\"estaDisponible\":true,\"sePuedeReservar\":true}]",
                MediaType.APPLICATION_JSON));

    var equipos = new CatalogoCliente(constructor.build()).listarEquipos();

    assertEquals("Xbox 360 #1", equipos.getFirst().nombre());
    servidor.verify();
  }

  @Test
  @DisplayName("CP-38h El token se reenvía al proveedor de reservas y su 409 llega con su mensaje")
  void reservasReenviaTokenYTraduceErrores() {
    RestClient.Builder constructor = RestClient.builder().baseUrl("http://reservas");
    MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
    servidor
        .expect(requestTo("http://reservas/api/reservas"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer abc"))
        .andRespond(
            withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    "{\"status\":409,\"mensaje\":\"El bloque ya está reservado para ese equipo\","
                        + "\"detalles\":[]}"));
    ReservasCliente cliente = new ReservasCliente(constructor.build());
    CrearReservaDto reserva =
        new CrearReservaDto(
            3L, "Xbox Series S #2", 5000, 3000, Instant.parse("2099-03-02T21:00:00Z"), 60);

    ErrorProveedorException error =
        assertThrows(ErrorProveedorException.class, () -> cliente.crear(reserva, "Bearer abc"));

    assertEquals(409, error.getStatus());
    assertEquals("El bloque ya está reservado para ese equipo", error.getMessage());
    servidor.verify();
  }

  @Test
  @DisplayName("CP-38i Con el proveedor detenido (puerto sin servidor) la llamada da ServicioNoDisponible")
  void proveedorDetenidoDaServicioNoDisponible() {
    RestClient sinServidor = RestClient.builder().baseUrl("http://localhost:1").build();

    ServicioNoDisponibleException error =
        assertThrows(
            ServicioNoDisponibleException.class,
            () -> new CatalogoCliente(sinServidor).listarEquipos());

    assertEquals(
        "El servicio de catálogo no está disponible en este momento. Intenta de nuevo en unos"
            + " minutos.",
        error.getMessage());
  }
}
