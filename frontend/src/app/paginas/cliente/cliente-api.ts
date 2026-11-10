import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { URL_API } from '../../core/configuracion';
import {
  Catalogo,
  CheckoutWompi,
  Disponibilidad,
  PagoAnticipo,
  ProveedorPagos,
  Reserva,
  ResultadoSimulado,
  SaldoPuntos,
} from '../../core/modelos';

/**
 * CACS-17 y CACS-19 (HU-02, HU-03 y HU-04): llamadas de las pantallas del cliente. Llevan el
 * token gracias a tokenInterceptor; el backend solo deja ver y pagar las reservas propias.
 */
@Injectable({ providedIn: 'root' })
export class ClienteApi {
  private readonly http = inject(HttpClient);
  private readonly urlApi = inject(URL_API);

  /** Equipos con modalidad y tarifa, y las condiciones de pago del negocio. */
  catalogo(): Observable<Catalogo> {
    return this.http.get<Catalogo>(`${this.urlApi}/api/catalogo`);
  }

  /** Bloques de 30 minutos de un equipo el día indicado (AAAA-MM-DD, hora de Bogotá). */
  disponibilidad(equipoId: number, fecha: string): Observable<Disponibilidad> {
    const params = new HttpParams().set('equipoId', equipoId).set('fecha', fecha);
    return this.http.get<Disponibilidad>(`${this.urlApi}/api/reservas/disponibilidad`, {
      params,
    });
  }

  /** Crea la reserva en PENDIENTE_PAGO; el precio y el adelanto los calcula el backend. */
  crearReserva(equipoId: number, inicio: string, duracionMinutos: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.urlApi}/api/reservas`, {
      equipoId,
      inicio,
      duracionMinutos,
    });
  }

  reserva(reservaId: number): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.urlApi}/api/reservas/${reservaId}`);
  }

  /** Sin resultado simulado, el cuerpo va vacío y el proveedor simulado aprueba el pago. */
  pagarAdelanto(
    reservaId: number,
    resultadoSimulado: ResultadoSimulado | null,
  ): Observable<PagoAnticipo> {
    const cuerpo = resultadoSimulado ? { resultadoSimulado } : {};
    return this.http.post<PagoAnticipo>(`${this.urlApi}/api/reservas/${reservaId}/pago`, cuerpo);
  }

  /** CACS-21: proveedor de pagos activo, para saber si se paga con el simulado o con Wompi. */
  proveedorPagos(): Observable<ProveedorPagos> {
    return this.http
      .get<{ proveedor: ProveedorPagos }>(`${this.urlApi}/api/pagos/proveedor`)
      .pipe(map((respuesta) => respuesta.proveedor));
  }

  /** CACS-21: crea el pago PENDIENTE y devuelve la URL del Web Checkout de Wompi. */
  iniciarPagoWompi(reservaId: number): Observable<CheckoutWompi> {
    return this.http.post<CheckoutWompi>(`${this.urlApi}/api/reservas/${reservaId}/pago/wompi`, {});
  }

  /** CACS-21: al volver de Wompi, consulta la transacción y registra su resultado. */
  consultarPagoWompi(reservaId: number, idTransaccion: string): Observable<PagoAnticipo> {
    return this.http.get<PagoAnticipo>(
      `${this.urlApi}/api/reservas/${reservaId}/pago/wompi/${encodeURIComponent(idTransaccion)}`,
    );
  }

  misPuntos(): Observable<SaldoPuntos> {
    return this.http.get<SaldoPuntos>(`${this.urlApi}/api/puntos`);
  }
}
