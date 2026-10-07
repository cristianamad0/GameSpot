package com.usta.gamespot.repository;

import com.usta.gamespot.model.Equipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

  List<Equipo> findByModalidadIdAndEstaDisponibleTrue(Long modalidadId);
}
