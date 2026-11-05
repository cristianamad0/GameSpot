import { Routes } from '@angular/router';
import { adminGuard, sesionGuard } from './core/auth/guardas';

/**
 * CACS-15: rutas de la aplicación. Las pantallas de reservar, mis puntos y el panel muestran
 * "Próximamente" hasta que se hagan sus tarjetas (CACS-19, CACS-17 y CACS-18); al hacerlas, se
 * cambia loadComponent y se quita data.
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
    path: 'admin',
    title: 'Panel · Game Spot',
    canActivate: [adminGuard],
    data: { titulo: 'Panel del administrador', tarjeta: 'CACS-18' },
    loadComponent: () => import('./paginas/proximamente/proximamente').then((m) => m.Proximamente),
  },
  {
    path: '**',
    title: 'Página no encontrada · Game Spot',
    loadComponent: () =>
      import('./paginas/no-encontrada/no-encontrada').then((m) => m.NoEncontrada),
  },
];
