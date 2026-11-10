import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EstadoReservaEtiqueta } from '../../compartido/estado-reserva/estado-reserva';
import { PROVEEDOR_SIMULADO, REDIRIGIR } from '../../core/configuracion';
import { mensajeDeError } from '../../core/errores/mensaje-error';
import { fechaHoraNegocio, horaNegocio, pesos, tiempoRestante } from '../../core/formato';
import { PagoAnticipo, ProveedorPagos, Reserva, ResultadoSimulado } from '../../core/modelos';
import { ClienteApi } from '../cliente/cliente-api';

/**
 * CACS-17 (HU-03): el cliente paga el adelanto de una reserva suya en PENDIENTE_PAGO. Muestra el
 * monto, la fecha límite con cuenta regresiva y el resultado. Si el pago se rechaza, la reserva
 * sigue pendiente y se puede reintentar mientras no venza el plazo.
 *
 * CACS-21: si el backend cobra con Wompi, el botón lleva al Web Checkout de Wompi. Wompi devuelve
 * al cliente a esta misma página con `?id=<transacción>`, y entonces se consulta el resultado.
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
  private readonly redirigir = inject(REDIRIGIR);
  private readonly ruta = inject(ActivatedRoute);
  private readonly selectorSimuladoHabilitado = inject(PROVEEDOR_SIMULADO);

  protected readonly reserva = signal<Reserva | null>(null);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<string | null>(null);

  /** Proveedor activo en el backend; null mientras se consulta. */
  protected readonly proveedor = signal<ProveedorPagos | null>(null);
  protected readonly esWompi = computed(() => this.proveedor() === 'WOMPI');
  protected readonly esSimulado = computed(
    () => this.selectorSimuladoHabilitado && this.proveedor() === 'SIMULADO',
  );

  protected readonly resultadoSimulado = signal<ResultadoSimulado>('APROBADO');
  protected readonly pagando = signal(false);
  protected readonly consultandoWompi = signal(false);
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
    this.api.proveedorPagos().subscribe({
      next: (proveedor) => this.proveedor.set(proveedor),
      error: () => this.proveedor.set('SIMULADO'),
    });
    this.api.reserva(Number(this.id())).subscribe({
      next: (reserva) => {
        this.reserva.set(reserva);
        this.cargando.set(false);
        const idTransaccion = this.ruta.snapshot.queryParamMap.get('id');
        if (idTransaccion) {
          this.consultarWompi(idTransaccion);
        }
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
    if (this.esWompi()) {
      this.api.iniciarPagoWompi(reserva.id).subscribe({
        // El navegador sale hacia Wompi; "pagando" sigue activo para que no se pulse dos veces.
        next: (checkout) => this.redirigir(checkout.urlCheckout),
        error: (error) => {
          this.errorPago.set(mensajeDeError(error));
          this.pagando.set(false);
        },
      });
      return;
    }
    const resultado = this.esSimulado() ? this.resultadoSimulado() : null;
    this.api.pagarAdelanto(reserva.id, resultado).subscribe({
      next: (pago) => this.mostrarResultado(pago),
      error: (error) => {
        this.errorPago.set(mensajeDeError(error));
        this.pagando.set(false);
      },
    });
  }

  /** Consulta en Wompi la transacción con la que volvió el cliente. */
  protected consultarWompi(idTransaccion: string): void {
    const reserva = this.reserva();
    if (!reserva || this.consultandoWompi()) {
      return;
    }
    this.consultandoWompi.set(true);
    this.errorPago.set(null);
    this.api.consultarPagoWompi(reserva.id, idTransaccion).subscribe({
      next: (pago) => {
        this.mostrarResultado(pago);
        this.consultandoWompi.set(false);
      },
      error: (error) => {
        this.errorPago.set(mensajeDeError(error));
        this.consultandoWompi.set(false);
      },
    });
  }

  /** Id de la transacción de Wompi en la URL de retorno, si la hay. */
  protected idTransaccionWompi(): string | null {
    return this.ruta.snapshot.queryParamMap.get('id');
  }

  private mostrarResultado(pago: PagoAnticipo): void {
    const reserva = this.reserva()!;
    this.ultimoPago.set(pago);
    this.reserva.set({ ...reserva, estado: pago.estadoReserva, expiraEn: pago.expiraEn });
    this.pagando.set(false);
  }
}
