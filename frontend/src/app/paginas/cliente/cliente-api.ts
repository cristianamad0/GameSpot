import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_API } from '../../core/configuracion';
import { PagoAnticipo, Reserva, ResultadoSimulado, SaldoPuntos } from '../../core/modelos';

/**
 * CACS-17 (HU-03 y HU-04): llamadas de las pantallas del cliente. Llevan el token gracias a
 * tokenInterceptor; el backend solo deja ver y pagar las reservas propias.
 */
@Injectable({ providedIn: 'root' })
export class ClienteApi {
  private readonly http = inject(HttpClient);
  private readonly urlApi = inject(URL_API);

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

  misPuntos(): Observable<SaldoPuntos> {
    return this.http.get<SaldoPuntos>(`${this.urlApi}/api/puntos`);
  }
}
