package com.usta.gamespot.repository;

import com.usta.gamespot.model.Notificacion;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

  /** Las próximas 50 notificaciones que ya deben enviarse. */
  List<Notificacion> findTop50ByFueEnviadaFalseAndProgramadaParaBeforeOrderByProgramadaParaAsc(
      Instant ahora);
}
