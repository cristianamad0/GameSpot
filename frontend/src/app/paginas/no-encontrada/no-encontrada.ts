import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** CACS-15: página para rutas que no existen. */
@Component({
  selector: 'gs-no-encontrada',
  imports: [RouterLink],
  template: `
    <section class="gs-contenedor no-encontrada">
      <p class="no-encontrada__codigo" aria-hidden="true">404</p>
      <h1>Game over para esta página</h1>
      <p>La dirección que buscas no existe.</p>
      <a routerLink="/" class="gs-boton gs-boton--primario">Volver al inicio</a>
    </section>
  `,
  styles: `
    .no-encontrada {
      padding: 64px 16px;
      text-align: center;
    }
    .no-encontrada__codigo {
      margin: 0;
      font-family: var(--gs-fuente-titulo);
      font-size: 4.5rem;
      font-weight: 800;
      color: var(--gs-acento);
      text-shadow: 0 0 24px var(--gs-acento-brillo);
    }
    h1 {
      font-size: 1.5rem;
    }
    p {
      color: var(--gs-texto-suave);
      margin-bottom: 24px;
    }
  `,
})
export class NoEncontrada {}
