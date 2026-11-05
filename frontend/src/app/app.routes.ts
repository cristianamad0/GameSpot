import { Routes } from '@angular/router';
import { adminGuard, sesionGuard } from './core/auth/guardas';

/**
 * CACS-15: rutas de la aplicación. Reservar y mis puntos muestran "Próximamente" hasta que se
 * hagan sus tarjetas (CACS-19 y CACS-17); al hacerlas, se cambia loadComponent y se quita data.
 */
export const routes: Routes = [
  {
    path: '',
    title: 'Game Spot · Reservas',
    loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio),
  },
  {
    path: 'ingresar',
    title: 'Ingresar · Game Spot',
    loadComponent: () => import('./paginas/ingresar/ingresar').then((m) => m.Ingresar),
  },
  {
    path: 'registro',
    title: 'Crear cuenta · Game Spot',
    loadComponent: () => import('./paginas/registro/registro').then((m) => m.Registro),
  },
  {
    path: 'reservar',
    title: 'Reservar · Game Spot',
    canActivate: [sesionGuard],
    data: { titulo: 'Reservar un equipo', tarjeta: 'CACS-19' },
    loadComponent: () => import('./paginas/proximamente/proximamente').then((m) => m.Proximamente),
  },
  {
    path: 'mis-puntos',
    title: 'Mis puntos · Game Spot',
    canActivate: [sesionGuard],
    data: { titulo: 'Mis puntos', tarjeta: 'CACS-17' },
    loadComponent: () => import('./paginas/proximamente/proximamente').then((m) => m.Proximamente),
  },
  {
    // CACS-18: panel del administrador. La guarda cubre también las rutas hijas.
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./paginas/admin/admin').then((m) => m.Admin),
    children: [
      {
        path: '',
        title: 'Reservas del día · Panel · Game Spot',
        loadComponent: () =>
          import('./paginas/admin/reservas-dia/reservas-dia').then((m) => m.ReservasDia),
      },
      {
        path: 'expiradas',
        title: 'Expiradas con pago · Panel · Game Spot',
        loadComponent: () => import('./paginas/admin/expiradas/expiradas').then((m) => m.Expiradas),
      },
      {
        path: 'puntos',
        title: 'Ajustar puntos · Panel · Game Spot',
        loadComponent: () =>
          import('./paginas/admin/ajuste-puntos/ajuste-puntos').then((m) => m.AjustePuntos),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada · Game Spot',
    loadComponent: () =>
      import('./paginas/no-encontrada/no-encontrada').then((m) => m.NoEncontrada),
  },
];
