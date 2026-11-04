package com.usta.gamespot.service;

import com.usta.gamespot.dto.MovimientoPuntosResponse;
import com.usta.gamespot.dto.SaldoPuntosResponse;
import com.usta.gamespot.exception.SaldoPuntosInsuficienteException;
import com.usta.gamespot.exception.UsuarioNoEncontradoException;
import com.usta.gamespot.model.MovimientoPuntos;
import com.usta.gamespot.model.TipoMovimientoPuntos;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.MovimientoPuntosRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-7 (HU-04): saldo e historial de puntos de fidelización y ajustes manuales del
 * administrador.
 *
 * <p>Los puntos por horas jugadas los otorga la base de datos al completar una reserva (trigger
 * otorgar_puntos_al_completar), una sola vez por reserva. Cada movimiento actualiza el saldo
 * (trigger actualizar_puntos_fidelidad) y la base impide que quede negativo.
 */
@Service
@RequiredArgsConstructor
public class PuntosFidelidadService {

  private final UsuarioRepository usuarioRepository;
  private final MovimientoPuntosRepository movimientoPuntosRepository;
  private final EntityManager entityManager;

  /**
   * Devuelve el saldo y el historial de puntos del usuario autenticado.
   *
   * @param emailUsuario correo del usuario autenticado
   * @return el saldo y los movimientos, del más reciente al más antiguo
   */
  @Transactional(readOnly = true)
  public SaldoPuntosResponse consultarPropios(String emailUsuario) {
    Usuario usuario =
        usuarioRepository
            .findByEmail(emailUsuario)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    // El saldo lo mantiene la base con triggers: se lee el valor real.
    entityManager.refresh(usuario);
    return saldoDe(usuario);
  }

  /**
   * Registra un ajuste manual de puntos sobre un usuario. El saldo nunca puede quedar negativo.
   *
   * @param usuarioId id del usuario
   * @param puntos puntos a sumar (positivo) o restar (negativo); ya validado como distinto de cero
   * @param descripcion por qué se hace el ajuste; ya validada como no vacía
   * @return el saldo y el historial actualizados
   * @throws UsuarioNoEncontradoException si el usuario no existe
   * @throws SaldoPuntosInsuficienteException si el ajuste dejaría el saldo en negativo
   */
  @Transactional
  public SaldoPuntosResponse ajustar(Long usuarioId, int puntos, String descripcion) {
    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));
    // Relee el saldo real y bloquea al usuario (SELECT ... FOR UPDATE) hasta el final de la
    // transacción, para que dos ajustes simultáneos no validen el mismo saldo.
    entityManager.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
    if (usuario.getPuntosFidelidad() + puntos < 0) {
      throw new SaldoPuntosInsuficienteException(usuario.getPuntosFidelidad(), puntos);
    }

    movimientoPuntosRepository.saveAndFlush(
        MovimientoPuntos.builder()
            .usuario(usuario)
            .puntos(puntos)
            .tipo(TipoMovimientoPuntos.AJUSTE_ADMIN)
            .descripcion(descripcion.strip())
            .build());

    // El trigger de la base actualizó el saldo: se vuelve a leer.
    entityManager.refresh(usuario);
    return saldoDe(usuario);
  }

  private SaldoPuntosResponse saldoDe(Usuario usuario) {
    return new SaldoPuntosResponse(
        usuario.getId(),
        usuario.getPuntosFidelidad(),
        movimientoPuntosRepository.findByUsuarioIdOrderByCreadoEnDesc(usuario.getId()).stream()
            .map(MovimientoPuntosResponse::de)
            .toList());
  }
}
