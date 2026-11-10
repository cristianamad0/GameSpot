package com.usta.gamespot.repository;

import com.usta.gamespot.model.Equipo;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

  List<Equipo> findByModalidadIdAndEstaDisponibleTrue(Long modalidadId);

  /** CACS-22: cuántos equipos tiene una modalidad. */
  long countByModalidadId(Long modalidadId);

  /**
   * CACS-4: lee el equipo con su modalidad y lo bloquea (SELECT ... FOR UPDATE) hasta que
   * termine la transacción. Así, dos reservas simultáneas del mismo equipo se atienden una
   * después de la otra: la segunda ya ve la primera y responde 409, en vez de chocar en la
   * restricción reservas_sin_cruces, donde PostgreSQL puede abortar una con un deadlock.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Equipo e join fetch e.modalidad where e.id = :id")
  Optional<Equipo> buscarParaReservar(@Param("id") Long id);

  /** CACS-19: todos los equipos con su modalidad, del más económico al más costoso. */
  @Query("select e from Equipo e join fetch e.modalidad m order by m.tarifaHoraPesos, e.nombre")
  List<Equipo> buscarConModalidad();
}
