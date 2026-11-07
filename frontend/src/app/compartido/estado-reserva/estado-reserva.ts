import { Component, computed, input } from '@angular/core';
import { EstadoReserva } from '../../core/modelos';

const ETIQUETAS: Record<EstadoReserva, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  CONFIRMADA: 'Confirmada',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
};

/**
 * CACS-15: etiqueta del estado de una reserva. Siempre muestra el texto además del color, para
 * que el estado se entienda sin distinguir colores. La usan las pantallas de CACS-17, 18 y 19.
 */
@Component({
  selector: 'gs-estado-reserva',
  template: `<span class="estado" [attr.data-estado]="estado()">{{ etiqueta() }}</span>`,
  styles: `
    .estado {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 0.85rem;
      font-weight: 600;
      border: 1px solid currentColor;
      white-space: nowrap;
    }
    .estado::before {
      content: '';
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: currentColor;
    }
    [data-estado='PENDIENTE_PAGO'] {
      color: var(--gs-aviso);
    }
    [data-estado='CONFIRMADA'] {
      color: var(--gs-acento);
    }
    [data-estado='COMPLETADA'] {
      color: var(--gs-exito);
    }
    [data-estado='CANCELADA'] {
      color: var(--gs-texto-suave);
    }
    [data-estado='EXPIRADA'] {
      color: var(--gs-error);
    }
  `,
})
export class EstadoReservaEtiqueta {
  readonly estado = input.required<EstadoReserva>();
  protected readonly etiqueta = computed(() => ETIQUETAS[this.estado()]);
}
