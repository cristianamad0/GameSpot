package com.usta.gamespot.service;

import com.usta.gamespot.dto.UsuarioResumenResponse;
import com.usta.gamespot.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-18 (HU-06): búsqueda de usuarios para el panel de administración. Hoy la usa el ajuste de
 * puntos, que necesita el id del cliente.
 */
@Service
@RequiredArgsConstructor
public class AdminUsuariosService {

  private final UsuarioRepository usuarioRepository;

  /**
   * Busca usuarios cuyo correo contiene el texto, sin distinguir mayúsculas.
   *
   * @param texto parte del correo; ya validado con al menos 3 caracteres
   * @return hasta 10 usuarios ordenados por correo; lista vacía si no hay coincidencias
   */
  @Transactional(readOnly = true)
  public List<UsuarioResumenResponse> buscarPorCorreo(String texto) {
    return usuarioRepository.findTop10ByEmailContainingIgnoreCaseOrderByEmailAsc(texto.strip())
        .stream()
        .map(UsuarioResumenResponse::de)
        .toList();
  }
}
