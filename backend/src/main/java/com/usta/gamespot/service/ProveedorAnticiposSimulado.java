package com.usta.gamespot.service;

import com.usta.gamespot.dto.ResultadoSimulado;
import com.usta.gamespot.model.EstadoPago;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * CACS-5 (HU-05): proveedor de anticipos simulado. No mueve dinero ni pide datos de tarjeta:
 * responde APROBADO o RECHAZADO según lo que indique la solicitud (APROBADO si no indica nada).
 */
@Component
public class ProveedorAnticiposSimulado implements ProveedorAnticipos {

  /** Valor que queda en pagos.metodo_pago para los cobros simulados. */
  public static final String METODO_SIMULADO = "SIMULADO";

  @Override
  public ResultadoCobro cobrar(SolicitudCobro solicitud) {
    EstadoPago estado =
        solicitud.resultadoSimulado() == ResultadoSimulado.RECHAZADO
            ? EstadoPago.RECHAZADO
            : EstadoPago.APROBADO;
    return new ResultadoCobro(estado, "SIM-" + UUID.randomUUID(), METODO_SIMULADO);
  }
}
