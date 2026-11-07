import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** CACS-18: estructura del panel del administrador, con pestañas para cada sección. */
@Component({
  selector: 'gs-admin',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <section class="gs-contenedor panel">
      <header class="panel__encabezado">
        <p class="panel__etiqueta">Administración</p>
        <h1>Panel de Game Spot</h1>
      </header>

      <nav class="pestanas" aria-label="Secciones del panel">
        <a
          routerLink="/admin"
          routerLinkActive="activa"
          [routerLinkActiveOptions]="{ exact: true }"
          ariaCurrentWhenActive="page"
          >Reservas del día</a
        >
        <a routerLink="/admin/expiradas" routerLinkActive="activa" ariaCurrentWhenActive="page"
          >Expiradas con pago</a
        >
        <a routerLink="/admin/puntos" routerLinkActive="activa" ariaCurrentWhenActive="page"
          >Ajustar puntos</a
        >
      </nav>

      <router-outlet />
    </section>
  `,
  styles: `
    .panel {
      padding: 32px 16px 64px;
    }
    .panel__etiqueta {
      margin: 0 0 4px;
      color: var(--gs-acento);
      font-weight: 600;
      font-size: 0.9rem;
    }
    h1 {
      font-size: clamp(1.5rem, 4vw, 2rem);
    }
    .pestanas {
      display: flex;
      gap: 6px;
      margin: 8px 0 24px;
      padding: 6px;
      border: 1px solid var(--gs-borde);
      border-radius: var(--gs-radio);
      background: var(--gs-superficie);
      flex-wrap: wrap;
    }
    .pestanas a {
      flex: 1 1 auto;
      justify-content: center;
      display: inline-flex;
      align-items: center;
      min-height: 44px;
      padding: 0 16px;
      border-radius: var(--gs-radio-pequeno);
      color: var(--gs-texto-suave);
      font-weight: 600;
      text-decoration: none;
      transition:
        background var(--gs-transicion),
        color var(--gs-transicion);
    }
    .pestanas a:hover {
      color: var(--gs-texto);
    }
    .pestanas a.activa {
      background: var(--gs-primario);
      color: #fff;
      box-shadow: 0 0 18px var(--gs-primario-brillo);
    }
  `,
})
export class Admin {}
