import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoReservaEtiqueta } from '../../compartido/estado-reserva/estado-reserva';
import { PROVEEDOR_SIMULADO } from '../../core/configuracion';
import { mensajeDeError } from '../../core/errores/mensaje-error';
import { fechaHoraNegocio, horaNegocio, pesos, tiempoRestante } from '../../core/formato';
import { PagoAnticipo, Reserva, ResultadoSimulado } from '../../core/modelos';
import { ClienteApi } from '../cliente/cliente-api';

/**
 * CACS-17 (HU-03): el cliente paga el adelanto de una reserva suya en PENDIENTE_PAGO. Muestra el
 * monto, la fecha límite con cuenta regresiva y el resultado. Si el pago se rechaza, la reserva
 * sigue pendiente y se puede reintentar mientras no venza el plazo.
 */
@Component({
  selector: 'gs-pagar-adelanto',
  imports: [EstadoReservaEtiqueta, RouterLink],
  templateUrl: './pagar-adelanto.html',
  styleUrl: './pagar-adelanto.scss',
})
export class PagarAdelanto implements OnInit {
  /** Id de la reserva, desde la ruta /reservas/:id/pagar. */
  readonly id = input.required<string>();

  private readonly api = inject(ClienteApi);
  protected readonly esSimulado = inject(PROVEEDOR_SIMULADO);

  protected readonly reserva = signal<Reserva | null>(null);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<string | null>(null);

  protected readonly resultadoSimulado = signal<ResultadoSimulado>('APROBADO');
  protected readonly pagando = signal(false);
  protected readonly errorPago = signal<string | null>(null);
  protected readonly ultimoPago = signal<PagoAnticipo | null>(null);

  /** Se actualiza cada segundo para la cuenta regresiva. */
  private readonly ahora = signal(Date.now());

  protected readonly restanteMs = computed(() => {
    const expiraEn = this.reserva()?.expiraEn;
    return expiraEn ? Date.parse(expiraEn) - this.ahora() : null;
  });
  protected readonly estaPendiente = computed(() => this.reserva()?.estado === 'PENDIENTE_PAGO');
  protected readonly plazoVencido = computed(
    () => this.estaPendiente() && (this.restanteMs() ?? 0) <= 0,
  );
  protected readonly sePuedePagar = computed(() => this.estaPendiente() && !this.plazoVencido());
  protected readonly saldoEnLocal = computed(() => {
    const reserva = this.reserva();
    return reserva ? reserva.precioTotalPesos - reserva.adelantoPesos : 0;
  });

  protected readonly pesos = pesos;
  protected readonly fechaHora = fechaHoraNegocio;
  protected readonly hora = horaNegocio;
  protected readonly tiempoRestante = tiempoRestante;

  constructor() {
    const reloj = setInterval(() => this.ahora.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(reloj));
  }

  ngOnInit(): void {
    this.api.reserva(Number(this.id())).subscribe({
      next: (reserva) => {
        this.reserva.set(reserva);
        this.cargando.set(false);
      },
      error: (error) => {
        this.errorCarga.set(mensajeDeError(error));
        this.cargando.set(false);
      },
    });
  }

  protected pagar(): void {
    const reserva = this.reserva();
    if (!reserva || !this.sePuedePagar() || this.pagando()) {
      return;
    }
    this.pagando.set(true);
    this.errorPago.set(null);
    this.ultimoPago.set(null);
    const resultado = this.esSimulado ? this.resultadoSimulado() : null;
    this.api.pagarAdelanto(reserva.id, resultado).subscribe({
      next: (pago) => {
        this.ultimoPago.set(pago);
        this.reserva.set({ ...reserva, estado: pago.estadoReserva, expiraEn: pago.expiraEn });
        this.pagando.set(false);
      },
      error: (error) => {
        this.errorPago.set(mensajeDeError(error));
        this.pagando.set(false);
      },
    });
  }
}
