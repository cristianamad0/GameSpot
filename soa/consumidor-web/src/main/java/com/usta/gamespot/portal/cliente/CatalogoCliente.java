package com.usta.gamespot.portal.cliente;

import com.usta.gamespot.portal.dto.EquipoDto;
import com.usta.gamespot.portal.dto.HorarioDto;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** CACS-27: llamadas HTTP al proveedor de catálogo (puerto 8091) con RestClient. */
@Component
public class CatalogoCliente {

  static final String SERVICIO = "catálogo";

  private final RestClient restClient;

  /**
   * Crea el cliente.
   *
   * @param restClient RestClient con la URL base del catálogo
   */
  public CatalogoCliente(@Qualifier("clienteCatalogo") RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * GET /api/equipos.
   *
   * @return los equipos
   */
  public List<EquipoDto> listarEquipos() {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .get()
                .uri("/api/equipos")
                .retrieve()
                .body(new ParameterizedTypeReference<List<EquipoDto>>() {}));
  }

  /**
   * GET /api/equipos/{id}.
   *
   * @param id id del equipo
   * @return el equipo
   */
  public EquipoDto obtenerEquipo(Long id) {
    return LlamadaProveedor.ejecutar(
        SERVICIO, () -> restClient.get().uri("/api/equipos/{id}", id).retrieve().body(EquipoDto.class));
  }

  /**
   * GET /api/horarios.
   *
   * @return el horario de cada día
   */
  public List<HorarioDto> listarHorarios() {
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .get()
                .uri("/api/horarios")
                .retrieve()
                .body(new ParameterizedTypeReference<List<HorarioDto>>() {}));
  }

  /**
   * PUT /api/equipos/{id} con el token del administrador.
   *
   * @param equipo equipo actual (nombre y modalidad se conservan)
   * @param estaDisponible nuevo estado
   * @param autorizacion encabezado Authorization recibido del navegador
   * @return el equipo actualizado
   */
  public EquipoDto cambiarDisponibilidad(
      EquipoDto equipo, boolean estaDisponible, String autorizacion) {
    Map<String, Object> cuerpo =
        Map.of(
            "nombre", equipo.nombre(),
            "modalidadId", equipo.modalidadId(),
            "estaDisponible", estaDisponible);
    return LlamadaProveedor.ejecutar(
        SERVICIO,
        () ->
            restClient
                .put()
                .uri("/api/equipos/{id}", equipo.id())
                .header("Authorization", autorizacion)
                .body(cuerpo)
                .retrieve()
                .body(EquipoDto.class));
  }

  /**
   * Comprueba si el proveedor responde.
   *
   * @return true si respondió a GET /api/horarios
   */
  public boolean estaDisponible() {
    try {
      listarHorarios();
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }
}
