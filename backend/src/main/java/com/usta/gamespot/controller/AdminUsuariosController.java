package com.usta.gamespot.controller;

import com.usta.gamespot.dto.UsuarioResumenResponse;
import com.usta.gamespot.service.AdminUsuariosService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CACS-18 (HU-06): búsqueda de usuarios desde el panel. Va bajo /api/admin/**, así que exige el rol
 * ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuariosController {

  private final AdminUsuariosService adminUsuariosService;

  /**
   * Busca usuarios por una parte de su correo.
   *
   * @param email texto a buscar, de al menos 3 caracteres
   * @return 200 con hasta 10 usuarios (lista vacía si no hay coincidencias); 400 si el texto es
   *     muy corto
   */
  @GetMapping
  public List<UsuarioResumenResponse> buscar(
      @RequestParam @NotBlank @Size(min = 3, max = 160) String email) {
    return adminUsuariosService.buscarPorCorreo(email);
  }
}
