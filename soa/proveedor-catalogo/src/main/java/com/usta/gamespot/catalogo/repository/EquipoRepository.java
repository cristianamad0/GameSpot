package com.usta.gamespot.catalogo.repository;

import com.usta.gamespot.catalogo.model.Equipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de la tabla equipos (patrón Repository de Spring Data JPA). */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

  /** Todos los equipos, del más económico al más costoso y luego por nombre. */
  List<Equipo> findAllByOrderByModalidadTarifaHoraPesosAscNombreAsc();

  boolean existsByNombreIgnoreCase(String nombre);

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
