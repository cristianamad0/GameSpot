package com.usta.gamespot.repository;

import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Reserva;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  /**
   * CACS-5: lee la reserva con su cliente y la bloquea (SELECT ... FOR UPDATE) hasta que termine
   * la transacción. Así, dos pagos simultáneos de la misma reserva se atienden uno después del
   * otro y el segundo ya ve la reserva confirmada.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Reserva r join fetch r.usuario where r.id = :id")
  Optional<Reserva> buscarParaPagar(@Param("id") Long id);

  /** Historial del cliente, de la más reciente a la más antigua. */
  List<Reserva> findByUsuarioIdOrderByInicioDesc(Long usuarioId);

  /**
   * CACS-22: cuántas reservas de un equipo están en alguno de los estados y empiezan después del
   * instante indicado. Sirve para advertir al administrador antes de poner un equipo fuera de
   * servicio.
   */
  long countByEquipoIdAndEstadoInAndInicioAfter(
      Long equipoId, Collection<EstadoReserva> estados, Instant inicio);

  /** Panel de administración: reservas por estado. */
  List<Reserva> findByEstadoOrderByInicioAsc(EstadoReserva estado);

  /**
   * Panel de administración: reservas que empiezan en [desde, hasta), con su cliente y su equipo
   * ya cargados, ordenadas por hora de inicio y equipo.
   */
  @Query(
      """
      select r from Reserva r
      join fetch r.usuario
      join fetch r.equipo e
      where r.inicio >= :desde
        and r.inicio < :hasta
      order by r.inicio, e.id
      """)
  List<Reserva> buscarQueInicianEntre(
      @Param("desde") Instant desde, @Param("hasta") Instant hasta);

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
