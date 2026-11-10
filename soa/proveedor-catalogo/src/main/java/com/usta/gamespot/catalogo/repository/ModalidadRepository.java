package com.usta.gamespot.catalogo.repository;

import com.usta.gamespot.catalogo.model.Modalidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de la tabla modalidades. */
public interface ModalidadRepository extends JpaRepository<Modalidad, Long> {

  List<Modalidad> findAllByOrderByTarifaHoraPesosAscNombreAsc();
}
