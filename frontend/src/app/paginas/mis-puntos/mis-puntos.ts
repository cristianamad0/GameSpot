import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { mensajeDeError } from '../../core/errores/mensaje-error';
import { fechaHoraNegocio, textoPuntos } from '../../core/formato';
import { MovimientoPuntos, SaldoPuntos } from '../../core/modelos';
import { ClienteApi } from '../cliente/cliente-api';

const TIPOS: Record<MovimientoPuntos['tipo'], string> = {
  HORAS_JUGADAS: 'Horas jugadas',
  AJUSTE_ADMIN: 'Ajuste del administrador',
};

/**
 * CACS-17 (HU-04): saldo de puntos del cliente y su historial, del más reciente al más antiguo.
 * Cada movimiento muestra el signo y la palabra "puntos", no solo el color.
 */
@Component({
  selector: 'gs-mis-puntos',
  imports: [RouterLink],
  templateUrl: './mis-puntos.html',
  styleUrl: './mis-puntos.scss',
})
export class MisPuntos implements OnInit {
  private readonly api = inject(ClienteApi);

  protected readonly saldo = signal<SaldoPuntos | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly movimientos = computed(() =>
    [...(this.saldo()?.movimientos ?? [])].sort(
      (a, b) => Date.parse(b.fecha) - Date.parse(a.fecha),
    ),
  );

  protected readonly fechaHora = fechaHoraNegocio;
  protected readonly textoPuntos = textoPuntos;
  protected readonly tipos = TIPOS;

  ngOnInit(): void {
    this.api.misPuntos().subscribe({
      next: (saldo) => {
        this.saldo.set(saldo);
        this.cargando.set(false);
      },
      error: (error) => {
        this.error.set(mensajeDeError(error));
        this.cargando.set(false);
      },
    });
  }

  /** "+2 puntos" o "−3 puntos": el signo va en el texto. */
  protected conSigno(puntos: number): string {
    return `${puntos > 0 ? '+' : '−'}${textoPuntos(Math.abs(puntos))}`;
  }
}
