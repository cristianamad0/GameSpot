import { Component, OnInit, inject, signal } from '@angular/core';
import { EstadoReservaEtiqueta } from '../../../compartido/estado-reserva/estado-reserva';
import { mensajeDeError } from '../../../core/errores/mensaje-error';
import { fechaHoraNegocio, horaNegocio, pesos } from '../../../core/formato';
import { ReservaExpiradaConPago } from '../../../core/modelos';
import { AdminApi } from '../admin-api';

/**
 * CACS-18 (HU-06): reservas que expiraron aunque su adelanto quedó aprobado (el pago llegó tarde).
 * El administrador las resuelve a mano: devolver el dinero o reubicar al cliente.
 */
@Component({
  selector: 'gs-expiradas',
  imports: [EstadoReservaEtiqueta],
  template: `
    <p class="explicacion">
      Reservas que vencieron aunque el cliente pagó el adelanto. Resuélvelas a mano: devuelve el
      dinero o reubica al cliente en otro bloque.
    </p>

    @if (cargando()) {
      <p aria-live="polite">Cargando…</p>
    } @else if (error()) {
      <div class="gs-alerta" role="alert">{{ error() }}</div>
    } @else if (casos().length === 0) {
      <div class="gs-tarjeta vacio">
        <span aria-hidden="true">✅</span>
        <p>No hay casos pendientes. Todo está al día.</p>
      </div>
    } @else {
      <ul class="casos">
        @for (caso of casos(); track caso.referenciaPago) {
          <li class="gs-tarjeta caso">
            <div class="caso__fila">
              <h2>{{ caso.reserva.nombreEquipo }}</h2>
              <gs-estado-reserva [estado]="caso.reserva.estado" />
            </div>
            <p>
              <strong>{{ caso.reserva.nombreCliente }}</strong>
              <span class="suave"> · {{ caso.reserva.emailCliente }}</span>
            </p>
            <p class="suave">
              Reserva: {{ fechaHora(caso.reserva.inicio) }} – {{ hora(caso.reserva.fin) }}
            </p>
            <dl class="pago">
              <div>
                <dt>Pagado</dt>
                <dd>{{ pesos(caso.montoPagadoCentavos / 100) }}</dd>
              </div>
              <div>
                <dt>Pago aprobado</dt>
                <dd>{{ fechaHora(caso.pagoAprobadoEn) }}</dd>
              </div>
              <div>
                <dt>Referencia</dt>
                <dd class="referencia">{{ caso.referenciaPago }}</dd>
              </div>
            </dl>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .explicacion,
    .suave {
      color: var(--gs-texto-suave);
    }
    .explicacion {
      margin: 0 0 18px;
    }
    .vacio {
      text-align: center;
      color: var(--gs-texto-suave);
    }
    .vacio span {
      font-size: 2rem;
    }
    .casos {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 14px;
      margin: 0;
      padding: 0;
      list-style: none;
    }
    .caso {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 18px;
    }
    .caso p {
      margin: 0;
    }
    .caso__fila {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
    h2 {
      font-size: 1.05rem;
      margin: 0;
    }
    .pago {
      display: grid;
      gap: 6px;
      margin: 6px 0 0;
      padding-top: 10px;
      border-top: 1px solid var(--gs-borde);
    }
    .pago div {
      display: flex;
      justify-content: space-between;
      gap: 12px;
    }
    .pago dt {
      color: var(--gs-texto-suave);
    }
    .pago dd {
      margin: 0;
      font-weight: 600;
      text-align: right;
    }
    .referencia {
      font-family: ui-monospace, Consolas, monospace;
      overflow-wrap: anywhere;
    }
    @media (max-width: 760px) {
      .casos {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class Expiradas implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly fechaHora = fechaHoraNegocio;
  protected readonly hora = horaNegocio;
  protected readonly pesos = pesos;

  protected readonly casos = signal<ReservaExpiradaConPago[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.expiradasConPago().subscribe({
      next: (casos) => {
        this.casos.set(casos);
        this.cargando.set(false);
      },
      error: (error) => {
        this.error.set(mensajeDeError(error));
        this.cargando.set(false);
      },
    });
  }
}
