package com.usta.gamespot.repository;

import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

  /** Para ubicar el pago cuando llega el webhook de Wompi. */
  Optional<Pago> findByReferencia(String referencia);

  /** Panel de administración: adelantos recibidos (usar EstadoPago.APROBADO). */
  List<Pago> findByEstadoOrderByCreadoEnDesc(EstadoPago estado);

  List<Pago> findByReservaId(Long reservaId);

  /** CACS-24: si la reserva tiene, además de este pago, otro en el estado indicado. */
  boolean existsByReservaIdAndEstadoAndIdNot(Long reservaId, EstadoPago estado, Long id);

  /**
   * Panel de administración: pagos en {@code estadoPago} cuya reserva quedó en {@code
   * estadoReserva}, con la reserva, su cliente y su equipo ya cargados. Con APROBADO y EXPIRADA
   * da los casos que el administrador debe resolver a mano.
   */
  @Query(
      """
      select p from Pago p
      join fetch p.reserva r
      join fetch r.usuario
      join fetch r.equipo
      where p.estado = :estadoPago
        and r.estado = :estadoReserva
      order by r.inicio desc
      """)
  List<Pago> buscarPorEstadoYEstadoDeReserva(
      @Param("estadoPago") EstadoPago estadoPago,
      @Param("estadoReserva") EstadoReserva estadoReserva);
}
