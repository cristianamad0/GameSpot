package com.usta.gamespot.repository;

import com.usta.gamespot.model.HorarioAtencion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

  Optional<HorarioAtencion> findByDiaSemana(Short diaSemana);
}
