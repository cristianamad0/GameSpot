import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { mensajeDeError } from '../../core/errores/mensaje-error';
import { hoyEnNegocio, horaNegocio, pesos } from '../../core/formato';
import { BloqueDisponibilidad, Catalogo, EquipoCatalogo } from '../../core/modelos';
import { ClienteApi } from '../cliente/cliente-api';

/** Estado de un bloque en la grilla. Siempre se muestra con texto, no solo con color. */
type EstadoBloque = 'libre' | 'ocupado' | 'pasado';

interface BloqueVista extends BloqueDisponibilidad {
  estado: EstadoBloque;
}

const TEXTO_ESTADO: Record<EstadoBloque, string> = {
  libre: 'Libre',
  ocupado: 'Ocupado',
  pasado: 'Ya pasó',
};

/** Ícono según la modalidad: las salas se distinguen de las consolas. */
export function iconoModalidad(modalidad: string): string {
  return modalidad.toLowerCase().includes('sala') ? '🛋️' : '🎮';
}

/** Duración en palabras: "30 minutos", "1 hora", "1 hora 30 minutos", "2 horas". */
export function textoDuracion(minutos: number): string {
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  const textoHoras = horas === 0 ? '' : `${horas} ${horas === 1 ? 'hora' : 'horas'}`;
  const textoMinutos = resto === 0 ? '' : `${resto} minutos`;
  return [textoHoras, textoMinutos].filter(Boolean).join(' ');
}

/** Precio que calcula ReservaService: tarifa por hora proporcional a los minutos, hacia arriba. */
export function precioReserva(tarifaHoraPesos: number, duracionMinutos: number): number {
  return Math.ceil((tarifaHoraPesos * duracionMinutos) / 60);
}

/** Adelanto que calcula ReservaService: porcentaje del precio, redondeado hacia arriba. */
export function adelantoReserva(precioTotalPesos: number, porcentajeAdelanto: number): number {
  return Math.ceil((precioTotalPesos * porcentajeAdelanto) / 100);
}

/**
 * CACS-19 (HU-02): el cliente elige un equipo, ve sus bloques de 30 minutos del día, elige el
 * inicio y la duración (solo bloques libres seguidos), ve el precio y el adelanto, y al reservar
 * pasa a pagar el adelanto (CACS-17). Si el bloque se ocupó mientras elegía (409), lo avisa y
 * actualiza la grilla.
 */
@Component({
  selector: 'gs-reservar',
  templateUrl: './reservar.html',
  styleUrl: './reservar.scss',
})
export class Reservar implements OnInit {
  private readonly api = inject(ClienteApi);
  private readonly router = inject(Router);

  protected readonly catalogo = signal<Catalogo | null>(null);
  protected readonly errorCatalogo = signal<string | null>(null);

  protected readonly equipo = signal<EquipoCatalogo | null>(null);
  protected readonly hoy = hoyEnNegocio();
  protected readonly fecha = signal(this.hoy);

  protected readonly bloques = signal<BloqueDisponibilidad[] | null>(null);
  protected readonly cargandoBloques = signal(false);
  protected readonly errorBloques = signal<string | null>(null);

  protected readonly inicio = signal<string | null>(null);
  protected readonly duracionMinutos = signal(30);

  protected readonly reservando = signal(false);
  protected readonly errorReserva = signal<string | null>(null);

  /** Cuenta las peticiones de disponibilidad para ignorar respuestas viejas. */
  private consulta = 0;

  protected readonly vista = computed<BloqueVista[]>(() => {
    const ahora = Date.now();
    return (this.bloques() ?? []).map((bloque) => ({
      ...bloque,
      estado:
        Date.parse(bloque.inicio) <= ahora ? 'pasado' : bloque.estaLibre ? 'libre' : 'ocupado',
    }));
  });

  private readonly indiceInicio = computed(() =>
    this.vista().findIndex((bloque) => bloque.inicio === this.inicio()),
  );

  /** Duraciones posibles desde el inicio elegido: solo bloques libres seguidos. */
  protected readonly duraciones = computed(() => {
    const desde = this.indiceInicio();
    if (desde < 0) {
      return [];
    }
    const opciones: number[] = [];
    for (let i = desde; i < this.vista().length && this.vista()[i].estado === 'libre'; i++) {
      opciones.push((i - desde + 1) * 30);
    }
    return opciones;
  });

  protected readonly fin = computed(() => {
    const desde = this.indiceInicio();
    return desde < 0 ? null : this.vista()[desde + this.duracionMinutos() / 30 - 1]?.fin;
  });

  protected readonly precioTotalPesos = computed(() => {
    const equipo = this.equipo();
    return equipo ? precioReserva(equipo.tarifaHoraPesos, this.duracionMinutos()) : 0;
  });

  protected readonly adelantoPesos = computed(() =>
    adelantoReserva(this.precioTotalPesos(), this.catalogo()?.porcentajeAdelanto ?? 0),
  );

  protected readonly hora = horaNegocio;
  protected readonly pesos = pesos;
  protected readonly icono = iconoModalidad;
  protected readonly textoDuracion = textoDuracion;
  protected readonly textoEstado = TEXTO_ESTADO;

  ngOnInit(): void {
    this.api.catalogo().subscribe({
      next: (catalogo) => this.catalogo.set(catalogo),
      error: (error) => this.errorCatalogo.set(mensajeDeError(error)),
    });
  }

  protected elegirEquipo(equipo: EquipoCatalogo): void {
    if (!equipo.estaDisponible) {
      return;
    }
    this.equipo.set(equipo);
    this.errorReserva.set(null);
    this.cargarBloques();
  }

  protected cambiarFecha(fecha: string): void {
    if (!fecha) {
      return;
    }
    this.fecha.set(fecha);
    this.errorReserva.set(null);
    this.cargarBloques();
  }

  protected elegirInicio(bloque: BloqueVista): void {
    if (bloque.estado !== 'libre') {
      return;
    }
    this.inicio.set(bloque.inicio);
    // El aviso de un intento anterior (por ejemplo, el 409) ya no aplica al nuevo bloque.
    this.errorReserva.set(null);
    // Una hora si cabe; si no, media hora.
    this.duracionMinutos.set(this.duraciones().includes(60) ? 60 : 30);
  }

  protected cambiarDuracion(valor: string): void {
    this.duracionMinutos.set(Number(valor));
  }

  protected estaElegido(bloque: BloqueVista): boolean {
    const desde = this.indiceInicio();
    const indice = this.vista().indexOf(bloque);
    return desde >= 0 && indice >= desde && indice < desde + this.duracionMinutos() / 30;
  }

  protected etiquetaBloque(bloque: BloqueVista): string {
    const elegido = this.estaElegido(bloque) ? ', elegido' : '';
    return `${horaNegocio(bloque.inicio)} a ${horaNegocio(bloque.fin)}, ${TEXTO_ESTADO[bloque.estado].toLowerCase()}${elegido}`;
  }

  protected reservar(): void {
    const equipo = this.equipo();
    const inicio = this.inicio();
    if (!equipo || !inicio || this.reservando()) {
      return;
    }
    this.reservando.set(true);
    this.errorReserva.set(null);
    this.api.crearReserva(equipo.id, inicio, this.duracionMinutos()).subscribe({
      next: (reserva) => {
        this.reservando.set(false);
        void this.router.navigate(['/reservas', reserva.id, 'pagar']);
      },
      error: (error) => {
        this.errorReserva.set(mensajeDeError(error));
        this.reservando.set(false);
        if (error?.status === 409) {
          // Otro cliente tomó el bloque o el equipo salió de servicio: se muestra la grilla al día.
          this.cargarBloques();
        }
      },
    });
  }

  private cargarBloques(): void {
    const equipo = this.equipo();
    if (!equipo) {
      return;
    }
    const consulta = ++this.consulta;
    this.inicio.set(null);
    this.cargandoBloques.set(true);
    this.errorBloques.set(null);
    this.api.disponibilidad(equipo.id, this.fecha()).subscribe({
      next: (disponibilidad) => {
        if (consulta === this.consulta) {
          this.bloques.set(disponibilidad.bloques);
          this.cargandoBloques.set(false);
        }
      },
      error: (error) => {
        if (consulta === this.consulta) {
          this.bloques.set(null);
          this.errorBloques.set(mensajeDeError(error));
          this.cargandoBloques.set(false);
        }
      },
    });
  }
}
