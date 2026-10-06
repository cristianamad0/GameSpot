package com.usta.gamespot.repository;

import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  /** Historial del cliente, de la más reciente a la más antigua. */
  List<Reserva> findByUsuarioIdOrderByInicioDesc(Long usuarioId);

  /** Panel de administración: reservas por estado. */
  List<Reserva> findByEstadoOrderByInicioAsc(EstadoReserva estado);

  /**
   * Reservas de un equipo que ocupan algún bloque entre desde y hasta. Sirve para mostrar los
   * bloques libres y ocupados. Usar con EstadoReserva.QUE_OCUPAN_BLOQUE.
   */
  @Query(
      """
      select r from Reserva r
      where r.equipo.id = :equipoId
        and r.estado in :estados
        and r.inicio < :hasta
        and r.fin > :desde
      order by r.inicio
      """)
  List<Reserva> buscarQueSeCruzan(
      @Param("equipoId") Long equipoId,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta,
      @Param("estados") Collection<EstadoReserva> estados);

  /**
   * Libera las reservas que no se pagaron a tiempo y devuelve cuántas expiraron. Llamarlo desde
   * un método @Scheduled anotado con @Transactional.
   */
  @Query(value = "select public.expirar_reservas_pendientes()", nativeQuery = true)
  int expirarPendientes();
}
