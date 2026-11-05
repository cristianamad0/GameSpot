import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Sesion } from './sesion';

/** CACS-15: rutas para cualquier usuario autenticado. Sin sesión, lleva al inicio de sesión. */
export const sesionGuard: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(Sesion);
  if (sesion.token()) {
    return true;
  }
  return inject(Router).createUrlTree(['/ingresar'], { queryParams: { vuelta: estado.url } });
};

/** CACS-15: rutas /admin, solo para ADMIN. Un CLIENTE vuelve al inicio; sin sesión, al login. */
export const adminGuard: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  if (!sesion.token()) {
    return router.createUrlTree(['/ingresar'], { queryParams: { vuelta: estado.url } });
  }
  return sesion.esAdmin() ? true : router.createUrlTree(['/']);
};
