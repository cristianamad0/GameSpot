package com.usta.gamespot.repository;

import com.usta.gamespot.model.MovimientoPuntos;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos, Long> {

  /** Historial de puntos del usuario. El saldo está en Usuario.puntosFidelidad. */
  List<MovimientoPuntos> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
}
