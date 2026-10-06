package com.usta.gamespot.repository;

import com.usta.gamespot.model.Modalidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModalidadRepository extends JpaRepository<Modalidad, Long> {

  List<Modalidad> findByEstaActivaTrueOrderByTarifaHoraPesosAsc();
}
