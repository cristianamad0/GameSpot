package com.usta.gamespot.dto;

import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.Usuario;

/**
 * CACS-18 (HU-06): datos de un usuario que el administrador ve al buscarlo. No incluye la
 * contraseña ni ningún otro dato sensible.
 *
 * @param id id del usuario
 * @param nombre nombre del usuario
 * @param email correo del usuario
 * @param rol CLIENTE o ADMIN
 * @param puntosFidelidad saldo de puntos
 */
public record UsuarioResumenResponse(
    Long id, String nombre, String email, Rol rol, Integer puntosFidelidad) {

  /**
   * Convierte la entidad en la respuesta de la API.
   *
   * @param usuario usuario encontrado
   * @return el resumen para el administrador
   */
  public static UsuarioResumenResponse de(Usuario usuario) {
    return new UsuarioResumenResponse(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getEmail(),
        usuario.getRol(),
        usuario.getPuntosFidelidad());
  }
}
