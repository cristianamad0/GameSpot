import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_API } from '../../core/configuracion';
import {
  EquipoAdmin,
  ModalidadAdmin,
  ReservaAdmin,
  ReservaExpiradaConPago,
  SaldoPuntos,
  UsuarioResumen,
} from '../../core/modelos';

/**
 * CACS-18 y CACS-22 (HU-06 y HU-04): llamadas del panel del administrador. Todas van bajo
 * /api/admin/** y llevan el token gracias a tokenInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly urlApi = inject(URL_API);

  /** Reservas que empiezan el día indicado (AAAA-MM-DD, hora de Bogotá). */
  reservasDelDia(fecha: string): Observable<ReservaAdmin[]> {
    const params = new HttpParams().set('fecha', fecha);
    return this.http.get<ReservaAdmin[]>(`${this.urlApi}/api/admin/reservas`, { params });
  }

  cancelar(reservaId: number, motivo: string): Observable<ReservaAdmin> {
    return this.http.post<ReservaAdmin>(`${this.urlApi}/api/admin/reservas/${reservaId}/cancelar`, {
      motivo,
    });
  }

  completar(reservaId: number): Observable<ReservaAdmin> {
    return this.http.post<ReservaAdmin>(
      `${this.urlApi}/api/admin/reservas/${reservaId}/completar`,
      {},
    );
  }

  expiradasConPago(): Observable<ReservaExpiradaConPago[]> {
    return this.http.get<ReservaExpiradaConPago[]>(
      `${this.urlApi}/api/admin/reservas/expiradas-con-pago`,
    );
  }

  buscarUsuarios(email: string): Observable<UsuarioResumen[]> {
    const params = new HttpParams().set('email', email);
    return this.http.get<UsuarioResumen[]>(`${this.urlApi}/api/admin/usuarios`, { params });
  }

  /** CACS-22: equipos con modalidad, tarifa, estado y reservas activas futuras. */
  equipos(): Observable<EquipoAdmin[]> {
    return this.http.get<EquipoAdmin[]>(`${this.urlApi}/api/admin/equipos`);
  }

  /** CACS-22: pone un equipo fuera de servicio (false) o lo habilita (true). */
  cambiarDisponibilidad(equipoId: number, estaDisponible: boolean): Observable<EquipoAdmin> {
    return this.http.put<EquipoAdmin>(
      `${this.urlApi}/api/admin/equipos/${equipoId}/disponibilidad`,
      { estaDisponible },
    );
  }

  /** CACS-22: modalidades con su tarifa y cuántos equipos tienen. */
  modalidades(): Observable<ModalidadAdmin[]> {
    return this.http.get<ModalidadAdmin[]>(`${this.urlApi}/api/admin/modalidades`);
  }

  /** CACS-22: cambia la tarifa de una modalidad y la activa o desactiva. */
  actualizarModalidad(
    modalidadId: number,
    tarifaHoraPesos: number,
    estaActiva: boolean,
  ): Observable<ModalidadAdmin> {
    return this.http.put<ModalidadAdmin>(`${this.urlApi}/api/admin/modalidades/${modalidadId}`, {
      tarifaHoraPesos,
      estaActiva,
    });
  }

  ajustarPuntos(usuarioId: number, puntos: number, descripcion: string): Observable<SaldoPuntos> {
    return this.http.post<SaldoPuntos>(`${this.urlApi}/api/admin/usuarios/${usuarioId}/puntos`, {
      puntos,
      descripcion,
    });
  }
}
