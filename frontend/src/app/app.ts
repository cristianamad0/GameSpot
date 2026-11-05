import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BarraNavegacion } from './diseno/barra-navegacion/barra-navegacion';

/** CACS-15: estructura de la aplicación: barra de navegación, contenido y pie. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, BarraNavegacion],
  template: `
    <a class="saltar" href="#contenido">Saltar al contenido</a>
    <gs-barra-navegacion />
    <main id="contenido" tabindex="-1">
      <router-outlet />
    </main>
    <footer class="pie">
      <div class="gs-contenedor">Game Spot · Sala de videojuegos · Villavicencio</div>
    </footer>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    main {
      flex: 1;
      outline: none;
    }
    .saltar {
      position: absolute;
      left: 16px;
      top: -60px;
      z-index: 20;
      padding: 10px 14px;
      background: var(--gs-acento);
      color: var(--gs-fondo);
      font-weight: 700;
      border-radius: var(--gs-radio-pequeno);
    }
    .saltar:focus {
      top: 12px;
    }
    .pie {
      padding: 24px 0;
      border-top: 1px solid var(--gs-borde);
      color: var(--gs-texto-suave);
      font-size: 0.9rem;
      text-align: center;
    }
  `,
})
export class App {}
