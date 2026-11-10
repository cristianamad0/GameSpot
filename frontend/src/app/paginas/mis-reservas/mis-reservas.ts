import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoReservaEtiqueta } from '../../compartido/estado-reserva/estado-reserva';
import { mensajeDeError } from '../../core/errores/mensaje-error';
import { fechaHoraNegocio, horaNegocio, pesos } from '../../core/formato';
import { ESTADOS_RESERVA, EstadoReserva, HistorialReservas } from '../../core/modelos';
import { ClienteApi } from '../cliente/cliente-api';

const ETIQUETAS_FILTRO: Record<EstadoReserva, string> = {
  PENDIENTE_PAGO: 'Pendientes de pago',
  CONFIRMADA: 'Confirmadas',
  COMPLETADA: 'Completadas',
  CANCELADA: 'Canceladas',
  EXPIRADA: 'Expiradas',
};

/**
 * CACS-3 (HU-07): historial de reservas del cliente, de la más reciente a la más antigua, con
 * filtro por estado y de a 10 por página. Desde una reserva pendiente de pago se va a pagarla.
 */
@Component({
  selector: 'gs-mis-reservas',
  imports: [RouterLink, EstadoReservaEtiqueta],
  templateUrl: './mis-reservas.html',
  styleUrl: './mis-reservas.scss',
})
export class MisReservas implements OnInit {
  private readonly api = inject(ClienteApi);

  protected readonly historial = signal<HistorialReservas | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtroEstado = signal<EstadoReserva | null>(null);
  protected readonly pagina = signal(0);

  protected readonly hayAnterior = computed(() => this.pagina() > 0);
  protected readonly haySiguiente = computed(() => {
    const historial = this.historial();
    return historial !== null && this.pagina() + 1 < historial.totalPaginas;
  });

  protected readonly estados = ESTADOS_RESERVA;
  protected readonly etiquetasFiltro = ETIQUETAS_FILTRO;
  protected readonly fechaHora = fechaHoraNegocio;
  protected readonly hora = horaNegocio;
  protected readonly pesos = pesos;

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarFiltro(valor: string): void {
    this.filtroEstado.set(valor === '' ? null : (valor as EstadoReserva));
    this.pagina.set(0);
    this.cargar();
  }

  protected irAPagina(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  /** "1 reserva" o "12 reservas". */
  protected textoTotal(total: number): string {
    return total === 1 ? '1 reserva' : `${total} reservas`;
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.api.historial(this.filtroEstado(), this.pagina()).subscribe({
      next: (historial) => {
        this.historial.set(historial);
        this.cargando.set(false);
      },
      error: (error) => {
        this.error.set(mensajeDeError(error));
        this.cargando.set(false);
      },
    });
  }
}
