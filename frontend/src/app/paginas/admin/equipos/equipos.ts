import { Component, OnInit, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../../core/errores/mensaje-error';
import { pesos } from '../../../core/formato';
import { EquipoAdmin, ModalidadAdmin } from '../../../core/modelos';
import { AdminApi } from '../admin-api';

/** Lo que el administrador está editando de una modalidad, antes de guardarla. */
interface BorradorModalidad {
  tarifaHoraPesos: number | null;
  estaActiva: boolean;
}

/** "1 reserva futura" o "3 reservas futuras". */
export function textoReservasFuturas(cantidad: number): string {
  return cantidad === 1 ? '1 reserva futura' : `${cantidad} reservas futuras`;
}

/** Advertencia que concuerda en número: "Su reserva futura sigue activa" o "Sus 3 ... siguen". */
export function textoSiguenActivas(cantidad: number): string {
  return cantidad === 1
    ? 'Su reserva futura sigue activa'
    : `Sus ${textoReservasFuturas(cantidad)} siguen activas`;
}

/**
 * CACS-22 (HU-06): el administrador pone equipos fuera de servicio o los habilita, y cambia la
 * tarifa de cada modalidad o la desactiva. Si un equipo tiene reservas futuras, pide confirmación
 * y advierte que esas reservas siguen activas. Las tarifas nuevas solo aplican a reservas nuevas.
 */
@Component({
  selector: 'gs-equipos',
  templateUrl: './equipos.html',
  styleUrl: './equipos.scss',
})
export class Equipos implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly equipos = signal<EquipoAdmin[] | null>(null);
  protected readonly modalidades = signal<ModalidadAdmin[] | null>(null);
  protected readonly error = signal<string | null>(null);

  /** Equipo con reservas futuras que espera confirmación para salir de servicio. */
  protected readonly porConfirmar = signal<EquipoAdmin | null>(null);
  protected readonly cambiando = signal<number | null>(null);
  protected readonly avisoEquipos = signal<string | null>(null);
  protected readonly errorEquipos = signal<string | null>(null);

  protected readonly borradores = signal<Record<number, BorradorModalidad>>({});
  protected readonly guardando = signal<number | null>(null);
  protected readonly avisoTarifas = signal<string | null>(null);
  protected readonly errorTarifas = signal<string | null>(null);

  protected readonly pesos = pesos;
  protected readonly textoReservasFuturas = textoReservasFuturas;

  /** "Seguirá activa" o "Seguirán activas", según cuántas reservas futuras tenga. */
  protected seguiranActivas(cantidad: number): string {
    return cantidad === 1 ? 'Seguirá activa' : 'Seguirán activas';
  }

  ngOnInit(): void {
    this.cargarEquipos();
    this.api.modalidades().subscribe({
      next: (modalidades) => this.ponerModalidades(modalidades),
      error: (error) => this.error.set(mensajeDeError(error)),
    });
  }

  protected estadoEquipo(equipo: EquipoAdmin): string {
    if (!equipo.modalidadEstaActiva) {
      return 'Modalidad desactivada';
    }
    return equipo.estaDisponible ? 'Disponible' : 'Fuera de servicio';
  }

  protected alternar(equipo: EquipoAdmin): void {
    this.avisoEquipos.set(null);
    this.errorEquipos.set(null);
    if (equipo.estaDisponible && equipo.reservasActivasFuturas > 0) {
      this.porConfirmar.set(equipo);
      return;
    }
    this.cambiarDisponibilidad(equipo, !equipo.estaDisponible);
  }

  protected confirmarFueraDeServicio(): void {
    const equipo = this.porConfirmar();
    if (equipo) {
      this.cambiarDisponibilidad(equipo, false);
    }
  }

  protected cancelarConfirmacion(): void {
    this.porConfirmar.set(null);
  }

  protected cambiarTarifa(modalidadId: number, valor: string): void {
    const tarifa = valor === '' ? null : Number(valor);
    this.borradores.update((b) => ({
      ...b,
      [modalidadId]: { ...b[modalidadId], tarifaHoraPesos: tarifa },
    }));
  }

  protected cambiarActiva(modalidadId: number, estaActiva: boolean): void {
    this.borradores.update((b) => ({ ...b, [modalidadId]: { ...b[modalidadId], estaActiva } }));
  }

  protected tarifaInvalida(modalidadId: number): boolean {
    const tarifa = this.borradores()[modalidadId]?.tarifaHoraPesos;
    return tarifa === null || tarifa === undefined || !Number.isInteger(tarifa) || tarifa <= 0;
  }

  protected guardarModalidad(modalidad: ModalidadAdmin): void {
    const borrador = this.borradores()[modalidad.id];
    this.avisoTarifas.set(null);
    this.errorTarifas.set(null);
    if (!borrador || this.tarifaInvalida(modalidad.id)) {
      this.errorTarifas.set(
        `La tarifa de ${modalidad.nombre} debe ser un número entero mayor que 0.`,
      );
      return;
    }
    this.guardando.set(modalidad.id);
    this.api
      .actualizarModalidad(modalidad.id, borrador.tarifaHoraPesos!, borrador.estaActiva)
      .subscribe({
        next: (actualizada) => {
          this.modalidades.update(
            (lista) => lista?.map((m) => (m.id === actualizada.id ? actualizada : m)) ?? null,
          );
          this.avisoTarifas.set(
            `${actualizada.nombre}: ${pesos(actualizada.tarifaHoraPesos)} por hora` +
              `${actualizada.estaActiva ? '' : ', desactivada'}. ` +
              'Las reservas que ya existen conservan su precio.',
          );
          this.guardando.set(null);
          // La tarifa y el estado se ven también en la lista de equipos.
          this.cargarEquipos();
        },
        error: (error) => {
          this.errorTarifas.set(mensajeDeError(error));
          this.guardando.set(null);
        },
      });
  }

  private cambiarDisponibilidad(equipo: EquipoAdmin, estaDisponible: boolean): void {
    this.cambiando.set(equipo.id);
    this.api.cambiarDisponibilidad(equipo.id, estaDisponible).subscribe({
      next: (actualizado) => {
        this.equipos.update(
          (lista) => lista?.map((e) => (e.id === actualizado.id ? actualizado : e)) ?? null,
        );
        this.porConfirmar.set(null);
        this.cambiando.set(null);
        if (actualizado.estaDisponible) {
          this.avisoEquipos.set(`${actualizado.nombre} quedó disponible para reservar.`);
        } else {
          const reservas = actualizado.reservasActivasFuturas;
          this.avisoEquipos.set(
            `${actualizado.nombre} quedó fuera de servicio y ya no se puede reservar.` +
              (reservas > 0 ? ` ${textoSiguenActivas(reservas)}: avisa a los clientes.` : ''),
          );
        }
      },
      error: (error) => {
        this.errorEquipos.set(mensajeDeError(error));
        this.cambiando.set(null);
      },
    });
  }

  private cargarEquipos(): void {
    this.api.equipos().subscribe({
      next: (equipos) => this.equipos.set(equipos),
      error: (error) => this.error.set(mensajeDeError(error)),
    });
  }

  private ponerModalidades(modalidades: ModalidadAdmin[]): void {
    this.modalidades.set(modalidades);
    this.borradores.set(
      Object.fromEntries(
        modalidades.map((m) => [
          m.id,
          { tarifaHoraPesos: m.tarifaHoraPesos, estaActiva: m.estaActiva },
        ]),
      ),
    );
  }
}
