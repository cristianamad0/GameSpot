package com.usta.gamespot.controller;

import com.usta.gamespot.dto.CatalogoResponse;
import com.usta.gamespot.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-19 (HU-02): catálogo de equipos para la pantalla de reservar. Exige un token JWT válido
 * (ver SecurityConfig).
 */
@RestController
@RequiredArgsConstructor
public class CatalogoController {

  private final CatalogoService catalogoService;

  /**
   * Devuelve los equipos con su modalidad y tarifa, el porcentaje de adelanto y el plazo de pago.
   *
   * @return 200 con el catálogo
   */
  @GetMapping("/api/catalogo")
  public CatalogoResponse obtener() {
    return catalogoService.obtener();
  }
}
