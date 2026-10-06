package com.usta.gamespot.repository;

import com.usta.gamespot.model.ConfiguracionNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionNegocioRepository
    extends JpaRepository<ConfiguracionNegocio, Short> {

  /** Devuelve la única fila de configuración del negocio. */
  default ConfiguracionNegocio obtener() {
    return findById(ConfiguracionNegocio.ID_UNICO)
        .orElseThrow(
            () -> new IllegalStateException("Falta la fila de configuracion_negocio con id = 1"));
  }
}
