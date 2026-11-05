import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { URL_API } from '../configuracion';
import { Sesion } from './sesion';

/**
 * CACS-15: agrega el token a las peticiones hacia la API. Si una ruta protegida responde 401 (token
 * vencido o inválido), cierra la sesión y lleva al inicio de sesión, recordando a dónde volver.
 * Las rutas /api/auth/** no se tocan: un 401 en el login significa credenciales incorrectas.
 */
export const tokenInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const urlApi = inject(URL_API);
  const sesion = inject(Sesion);
  const router = inject(Router);

  const esDeLaApi = peticion.url.startsWith(urlApi);
  const esDeAutenticacion = peticion.url.startsWith(`${urlApi}/api/auth/`);
  if (!esDeLaApi || esDeAutenticacion) {
    return siguiente(peticion);
  }

  const token = sesion.token();
  const conToken = token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        sesion.cerrar();
        router.navigate(['/ingresar'], {
          queryParams: { vuelta: router.url, motivo: 'sesion-vencida' },
        });
      }
      return throwError(() => error);
    }),
  );
};
