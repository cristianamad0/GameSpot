import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * CACS-15: espacio reservado para las pantallas que llegan en otras tarjetas. Cada ruta le pasa
 * su título y la tarjeta que la implementa (por data de la ruta). Se reemplaza al hacerla.
 */
@Component({
  selector: 'gs-proximamente',
  imports: [RouterLink],
  template: `
    <section class="gs-contenedor proximamente">
      <div class="gs-tarjeta proximamente__tarjeta">
        <span class="proximamente__icono" aria-hidden="true">🚧</span>
        <h1>{{ titulo() }}</h1>
        <p>Esta pantalla está en construcción ({{ tarjeta() }}).</p>
        <a routerLink="/" class="gs-boton gs-boton--secundario">Volver al inicio</a>
      </div>
    </section>
  `,
  styles: `
    .proximamente {
      padding: 56px 16px;
    }
    .proximamente__tarjeta {
      max-width: 520px;
      margin: 0 auto;
      text-align: center;
    }
    .proximamente__icono {
      font-size: 2.4rem;
    }
    h1 {
      font-size: 1.5rem;
      margin-top: 8px;
    }
    p {
      color: var(--gs-texto-suave);
      margin-bottom: 24px;
    }
  `,
})
export class Proximamente {
  readonly titulo = input('Próximamente');
  readonly tarjeta = input('');
}
