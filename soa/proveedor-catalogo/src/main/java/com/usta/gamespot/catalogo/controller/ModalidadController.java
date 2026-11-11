package com.usta.gamespot.catalogo.controller;

import com.usta.gamespot.catalogo.dto.HorarioResponse;
import com.usta.gamespot.catalogo.dto.ModalidadResponse;
import com.usta.gamespot.catalogo.dto.TarifaRequest;
import com.usta.gamespot.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** CACS-25: modalidades con su tarifa y horario de atención. */
@RestController
@RequiredArgsConstructor
public class ModalidadController {

  private final CatalogoService catalogoService;

  /**
   * Lista las modalidades.
   *
   * @return 200 con las modalidades y su tarifa
   */
  @GetMapping("/api/modalidades")
  public List<ModalidadResponse> listar() {
    return catalogoService.listarModalidades();
  }

  /**
   * Cambia la tarifa de una modalidad. Exige un JWT de administrador.
   *
   * @param id id de la modalidad
   * @param request tarifa nueva y si se ofrece
   * @return 200 con la modalidad; 400 tarifa inválida; 404 si no existe
   */
  @PutMapping("/api/modalidades/{id}")
  public ModalidadResponse actualizarTarifa(
      @PathVariable Long id, @Valid @RequestBody TarifaRequest request) {
    return catalogoService.actualizarTarifa(id, request);
  }

  /**
   * Horario de atención de la semana.
   *
   * @return 200 con un horario por día
   */
  @GetMapping("/api/horarios")
  public List<HorarioResponse> horarios() {
    return catalogoService.listarHorarios();
  }
}
