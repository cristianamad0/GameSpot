import { Routes } from '@angular/router';
import { adminGuard, sesionGuard } from './core/auth/guardas';

/**
 * CACS-15: rutas de la aplicación. Reservar muestra "Próximamente" hasta que se haga su tarjeta
 * (CACS-19); al hacerla, se cambia loadComponent y se quita data.
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
    // CACS-17 (HU-04): saldo e historial de puntos del cliente.
    path: 'mis-puntos',
    title: 'Mis puntos · Game Spot',
    canActivate: [sesionGuard],
    loadComponent: () => import('./paginas/mis-puntos/mis-puntos').then((m) => m.MisPuntos),
  },
  {
    // CACS-17 (HU-03): pago del adelanto. CACS-19 lleva aquí al cliente después de reservar.
    path: 'reservas/:id/pagar',
    title: 'Pagar el adelanto · Game Spot',
    canActivate: [sesionGuard],
    loadComponent: () =>
      import('./paginas/pagar-adelanto/pagar-adelanto').then((m) => m.PagarAdelanto),
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
