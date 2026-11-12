package com.usta.gamespot.reservas.repository;

import com.usta.gamespot.reservas.model.EstadoReserva;
import com.usta.gamespot.reservas.model.Reserva;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de la tabla reservas. */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  /** Historial de un usuario, de la reserva más reciente a la más antigua. */
  List<Reserva> findByUsuarioEmailIgnoreCaseOrderByInicioDesc(String email);

  /**
   * Reservas activas de un equipo que se cruzan con el intervalo [desde, hasta). Una reserva está
   * activa si está confirmada o si está pendiente de pago y se creó después de {@code
   * creadasDesde} (CACS-47: las pendientes más viejas ya vencieron y no bloquean el turno). Sirve
   * para detectar cruces al reservar y para mostrar la ocupación de un día.
   */
  @Query(
      """
      select r from Reserva r
      where r.equipoId = :equipoId
        and (r.estado = :confirmada
             or (r.estado = :pendiente and r.creadaEn > :creadasDesde))
        and r.inicio < :hasta
        and r.fin > :desde
      order by r.inicio
      """)
  List<Reserva> buscarActivasQueSeCruzan(
      @Param("equipoId") Long equipoId,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta,
      @Param("creadasDesde") Instant creadasDesde,
      @Param("confirmada") EstadoReserva confirmada,
      @Param("pendiente") EstadoReserva pendiente);
}
