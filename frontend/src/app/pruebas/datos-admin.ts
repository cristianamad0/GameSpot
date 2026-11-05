import { EstadoReserva, ReservaAdmin } from '../core/modelos';

/** Reserva de prueba del 10 de noviembre de 2026 (hora de Bogotá = UTC-5). */
export function reservaDePrueba(
  id: number,
  equipoId: number,
  nombreEquipo: string,
  horaInicioUtc: string,
  estado: EstadoReserva,
): ReservaAdmin {
  const inicio = `2026-11-10T${horaInicioUtc}:00Z`;
  const fin = new Date(new Date(inicio).getTime() + 60 * 60 * 1000).toISOString();
  return {
    id,
    usuarioId: 100 + id,
    nombreCliente: `Cliente ${id}`,
    emailCliente: `cliente${id}@gamespot.test`,
    equipoId,
    nombreEquipo,
    inicio,
    fin,
    precioTotalPesos: 8000,
    adelantoPesos: 4000,
    estado,
    tieneAnticipoPagado: estado === 'CONFIRMADA' || estado === 'COMPLETADA',
    canceladaEn: null,
    motivoCancelacion: null,
  };
}

/** Escribe en un input o textarea y avisa a Angular. */
export function escribir(contenedor: HTMLElement, selector: string, valor: string): void {
  const campo = contenedor.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
  campo.dispatchEvent(new Event('blur'));
}

/** Elige la opción número {@code indice} de un select y avisa a Angular. */
export function elegirOpcion(contenedor: HTMLElement, selector: string, indice: number): void {
  const select = contenedor.querySelector<HTMLSelectElement>(selector)!;
  select.selectedIndex = indice;
  select.dispatchEvent(new Event('change'));
}

/** Busca un botón por su texto visible. */
export function botonCon(contenedor: HTMLElement, texto: string): HTMLButtonElement {
  const boton = Array.from(contenedor.querySelectorAll('button')).find((b) =>
    b.textContent?.includes(texto),
  );
  if (!boton) {
    throw new Error(`No hay un botón con el texto "${texto}"`);
  }
  return boton;
}
