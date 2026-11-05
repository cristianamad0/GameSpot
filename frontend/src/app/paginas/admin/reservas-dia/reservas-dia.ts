import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EstadoReservaEtiqueta } from '../../../compartido/estado-reserva/estado-reserva';
import { mensajeDeError } from '../../../core/errores/mensaje-error';
import { hoyEnNegocio, horaNegocio, pesos } from '../../../core/formato';
import { ESTADOS_RESERVA, EstadoReserva, ReservaAdmin } from '../../../core/modelos';
import { AdminApi } from '../admin-api';

type Accion = 'cancelar' | 'completar';

interface GrupoEquipo {
  equipoId: number;
  nombreEquipo: string;
  reservas: ReservaAdmin[];
}

const ETIQUETAS_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  CONFIRMADA: 'Confirmada',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
};

const SE_PUEDEN_CANCELAR: EstadoReserva[] = ['PENDIENTE_PAGO', 'CONFIRMADA'];

/**
 * CACS-18 (HU-06): reservas de un día agrupadas por equipo, con filtros por equipo y estado.
 * Permite cancelar con motivo y marcar como completada; la vista se actualiza con la respuesta
 * del backend, sin recargar.
 */
@Component({
  selector: 'gs-reservas-dia',
  imports: [FormsModule, EstadoReservaEtiqueta],
  templateUrl: './reservas-dia.html',
  styleUrl: './reservas-dia.scss',
})
export class ReservasDia implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly estados = ESTADOS_RESERVA;
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO;
  protected readonly hora = horaNegocio;
  protected readonly pesos = pesos;

  protected readonly fecha = signal(hoyEnNegocio());
  protected readonly filtroEquipo = signal<number | null>(null);
  protected readonly filtroEstado = signal<EstadoReserva | null>(null);

  protected readonly reservas = signal<ReservaAdmin[]>([]);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<string | null>(null);

  protected readonly accionAbierta = signal<{ id: number; tipo: Accion } | null>(null);
  protected readonly motivo = signal('');
  protected readonly procesando = signal<number | null>(null);
  protected readonly errorAccion = signal<{ id: number; mensaje: string } | null>(null);
  protected readonly aviso = signal<string | null>(null);

  /** Equipos con reservas en el día, para el filtro. */
  protected readonly equipos = computed(() => {
    const vistos = new Map<number, string>();
    for (const reserva of this.reservas()) {
      vistos.set(reserva.equipoId, reserva.nombreEquipo);
    }
    return [...vistos].map(([id, nombre]) => ({ id, nombre }));
  });

  /** Reservas filtradas y agrupadas por equipo, en orden de hora. */
  protected readonly grupos = computed<GrupoEquipo[]>(() => {
    const equipo = this.filtroEquipo();
    const estado = this.filtroEstado();
    const grupos = new Map<number, GrupoEquipo>();
    const filtradas = this.reservas()
      .filter((r) => equipo === null || r.equipoId === equipo)
      .filter((r) => estado === null || r.estado === estado)
      .sort((a, b) => a.inicio.localeCompare(b.inicio));
    for (const reserva of filtradas) {
      const grupo = grupos.get(reserva.equipoId) ?? {
        equipoId: reserva.equipoId,
        nombreEquipo: reserva.nombreEquipo,
        reservas: [],
      };
      grupo.reservas.push(reserva);
      grupos.set(reserva.equipoId, grupo);
    }
    return [...grupos.values()];
  });

  protected readonly totalFiltradas = computed(() =>
    this.grupos().reduce((total, grupo) => total + grupo.reservas.length, 0),
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarFecha(fecha: string): void {
    if (!fecha) {
      return;
    }
    this.fecha.set(fecha);
    this.filtroEquipo.set(null);
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.cerrarAccion();
    this.api.reservasDelDia(this.fecha()).subscribe({
      next: (reservas) => {
        this.reservas.set(reservas);
        this.cargando.set(false);
      },
      error: (error) => {
        this.reservas.set([]);
        this.errorCarga.set(mensajeDeError(error));
        this.cargando.set(false);
      },
    });
  }

  protected sePuedeCancelar(reserva: ReservaAdmin): boolean {
    return SE_PUEDEN_CANCELAR.includes(reserva.estado);
  }

  protected abrirAccion(reserva: ReservaAdmin, tipo: Accion): void {
    this.accionAbierta.set({ id: reserva.id, tipo });
    this.motivo.set('');
    this.errorAccion.set(null);
    this.aviso.set(null);
  }

  protected cerrarAccion(): void {
    this.accionAbierta.set(null);
    this.motivo.set('');
  }

  protected accionDe(reserva: ReservaAdmin): Accion | null {
    const abierta = this.accionAbierta();
    return abierta?.id === reserva.id ? abierta.tipo : null;
  }

  protected errorDe(reserva: ReservaAdmin): string | null {
    const error = this.errorAccion();
    return error?.id === reserva.id ? error.mensaje : null;
  }

  protected motivoValido(): boolean {
    const texto = this.motivo().trim();
    return texto.length > 0 && texto.length <= 500;
  }

  protected confirmar(reserva: ReservaAdmin): void {
    const accion = this.accionDe(reserva);
    if (!accion || (accion === 'cancelar' && !this.motivoValido())) {
      return;
    }
    this.procesando.set(reserva.id);
    this.errorAccion.set(null);
    const peticion =
      accion === 'cancelar'
        ? this.api.cancelar(reserva.id, this.motivo().trim())
        : this.api.completar(reserva.id);
    peticion.subscribe({
      next: (actualizada) => {
        this.reservas.update((lista) =>
          lista.map((r) => (r.id === actualizada.id ? actualizada : r)),
        );
        this.aviso.set(
          accion === 'cancelar'
            ? `Reserva de ${actualizada.nombreCliente} cancelada; el bloque quedó libre.`
            : `Reserva de ${actualizada.nombreCliente} completada; se sumaron sus puntos.`,
        );
        this.procesando.set(null);
        this.cerrarAccion();
      },
      error: (error) => {
        this.errorAccion.set({ id: reserva.id, mensaje: mensajeDeError(error) });
        this.procesando.set(null);
      },
    });
  }
}
