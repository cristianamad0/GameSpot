import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { URL_API } from '../configuracion';
import { RespuestaAutenticacion } from '../modelos';
import { Sesion } from './sesion';

export interface DatosIngreso {
  email: string;
  password: string;
}

export interface DatosRegistro extends DatosIngreso {
  nombre: string;
}

/** CACS-15 (HU-01): registro e inicio de sesión contra /api/auth. Abre la sesión al responder. */
@Injectable({ providedIn: 'root' })
export class AutenticacionApi {
  private readonly http = inject(HttpClient);
  private readonly urlApi = inject(URL_API);
  private readonly sesion = inject(Sesion);

  ingresar(datos: DatosIngreso): Observable<RespuestaAutenticacion> {
    return this.http
      .post<RespuestaAutenticacion>(`${this.urlApi}/api/auth/login`, datos)
      .pipe(tap((respuesta) => this.sesion.abrir(respuesta)));
  }

  registrar(datos: DatosRegistro): Observable<RespuestaAutenticacion> {
    return this.http
      .post<RespuestaAutenticacion>(`${this.urlApi}/api/auth/registro`, datos)
      .pipe(tap((respuesta) => this.sesion.abrir(respuesta)));
  }
}
