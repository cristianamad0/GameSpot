package com.usta.gamespot.repository;

import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.Pago;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

  /** Para ubicar el pago cuando llega el webhook de Wompi. */
  Optional<Pago> findByReferencia(String referencia);

  /** Panel de administración: adelantos recibidos (usar EstadoPago.APROBADO). */
  List<Pago> findByEstadoOrderByCreadoEnDesc(EstadoPago estado);

  List<Pago> findByReservaId(Long reservaId);
}
