package com.usta.gamespot.repository;

import com.usta.gamespot.model.Notificacion;
import com.usta.gamespot.model.TipoNotificacion;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

  /** Las próximas 50 notificaciones que ya deben enviarse. */
  List<Notificacion> findTop50ByFueEnviadaFalseAndProgramadaParaBeforeOrderByProgramadaParaAsc(
      Instant ahora);

  /**
   * CACS-8: notificaciones sin enviar cuya hora ya llegó, de la más antigua a la más reciente,
   * con su cliente y su reserva cargados. Los recordatorios solo se envían si la reserva sigue
   * CONFIRMADA: el de una reserva cancelada (o ya completada) nunca sale.
   */
  @Query(
      """
      select n from Notificacion n
      join fetch n.usuario
      left join fetch n.reserva r
      left join fetch r.equipo
      where n.fueEnviada = false
        and n.programadaPara <= :ahora
        and (n.tipo = com.usta.gamespot.model.TipoNotificacion.CONFIRMACION
             or r.estado = com.usta.gamespot.model.EstadoReserva.CONFIRMADA)
      order by n.programadaPara
      """)
  List<Notificacion> buscarPorEnviar(@Param("ahora") Instant ahora, Pageable pagina);

  /** CACS-8: si la reserva ya tiene una notificación de ese tipo. */
  boolean existsByReservaIdAndTipo(Long reservaId, TipoNotificacion tipo);

  /** CACS-8: notificaciones de una reserva, en el orden en que se programaron. */
  List<Notificacion> findByReservaIdOrderByProgramadaParaAsc(Long reservaId);
}
