package com.usta.gamespot.catalogo.repository;

import com.usta.gamespot.catalogo.model.HorarioAtencion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de la tabla horarios_atencion. */
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Short> {

  List<HorarioAtencion> findAllByOrderByDiaSemanaAsc();
}
