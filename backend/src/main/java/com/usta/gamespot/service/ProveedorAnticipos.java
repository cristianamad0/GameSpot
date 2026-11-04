package com.usta.gamespot.service;

import com.usta.gamespot.dto.ResultadoSimulado;
import com.usta.gamespot.model.EstadoPago;

/**
 * CACS-5 (HU-05): proveedor que cobra el adelanto de una reserva. Hoy lo implementa {@link
 * ProveedorAnticiposSimulado} (sin dinero real); más adelante se cambia por Wompi en entorno de
 * pruebas sin tocar {@link PagoAnticipoService}.
 *
 * <p>Ninguna implementación debe guardar llaves, secretos ni datos de tarjeta: las llaves se leen
 * de variables de entorno y los datos de tarjeta solo los maneja el proveedor.
 */
public interface ProveedorAnticipos {

  /**
   * Cobra el adelanto y devuelve el resultado de la transacción.
   *
   * @param solicitud referencia y monto a cobrar
   * @return el estado final de la transacción según el proveedor
   */
  ResultadoCobro cobrar(SolicitudCobro solicitud);

  /**
   * Datos que el sistema envía al proveedor.
   *
   * @param referencia referencia única del pago (la misma de pagos.referencia)
   * @param montoCentavos monto a cobrar, en centavos de peso
   * @param resultadoSimulado resultado que se le pide al proveedor simulado; un proveedor real lo
   *     ignora. Puede ser null.
   */
  record SolicitudCobro(
      String referencia, long montoCentavos, ResultadoSimulado resultadoSimulado) {}

  /**
   * Respuesta del proveedor.
   *
   * @param estado estado final de la transacción
   * @param idTransaccion id que asigna el proveedor a la transacción
   * @param metodoPago medio de pago usado (CARD, NEQUI, SIMULADO...)
   */
  record ResultadoCobro(EstadoPago estado, String idTransaccion, String metodoPago) {}
}
