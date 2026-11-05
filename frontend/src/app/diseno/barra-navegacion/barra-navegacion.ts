import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Sesion } from '../../core/auth/sesion';

/** CACS-15: barra superior con el logo, los enlaces según el rol y la sesión del usuario. */
@Component({
  selector: 'gs-barra-navegacion',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './barra-navegacion.html',
  styleUrl: './barra-navegacion.scss',
})
export class BarraNavegacion {
  protected readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  protected readonly menuAbierto = signal(false);

  protected alternarMenu(): void {
    this.menuAbierto.update((abierto) => !abierto);
  }

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
  }

  protected salir(): void {
    this.sesion.cerrar();
    this.cerrarMenu();
    this.router.navigate(['/']);
  }
}
