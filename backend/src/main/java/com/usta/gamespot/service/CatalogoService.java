package com.usta.gamespot.service;

import com.usta.gamespot.dto.CatalogoResponse;
import com.usta.gamespot.model.ConfiguracionNegocio;
import com.usta.gamespot.repository.ConfiguracionNegocioRepository;
import com.usta.gamespot.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-19 (HU-02): catálogo de equipos para la pantalla de reservar. Incluye el porcentaje de
 * adelanto para que el cliente vea el precio y el adelanto antes de confirmar; el valor que se
 * cobra lo vuelve a calcular ReservaService al crear la reserva.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

  private final EquipoRepository equipoRepository;
  private final ConfiguracionNegocioRepository configuracionNegocioRepository;

  /**
   * Devuelve los equipos con su modalidad y tarifa, y las condiciones de pago del negocio.
   *
   * @return el catálogo; los equipos fuera de servicio vienen con estaDisponible en false
   */
  @Transactional(readOnly = true)
  public CatalogoResponse obtener() {
    ConfiguracionNegocio configuracion = configuracionNegocioRepository.obtener();
    return new CatalogoResponse(
        configuracion.getPorcentajeAdelanto(),
        configuracion.getMinutosEsperaPago(),
        equipoRepository.buscarConModalidad().stream()
            .map(CatalogoResponse.EquipoCatalogo::de)
            .toList());
  }
}
