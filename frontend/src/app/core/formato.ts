/** Zona del negocio: Game Spot está en Villavicencio (UTC-5, sin horario de verano). */
export const ZONA_NEGOCIO = 'America/Bogota';

const formatoHora = new Intl.DateTimeFormat('es-CO', {
  timeZone: ZONA_NEGOCIO,
  hour: 'numeric',
  minute: '2-digit',
});

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', {
  timeZone: ZONA_NEGOCIO,
  dateStyle: 'medium',
  timeStyle: 'short',
});

const formatoPesos = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

/** Hora en Bogotá, por ejemplo "3:00 p. m.". */
export function horaNegocio(instante: string): string {
  return formatoHora.format(new Date(instante));
}

/** Fecha y hora en Bogotá, por ejemplo "10 nov 2026, 3:00 p. m.". */
export function fechaHoraNegocio(instante: string): string {
  return formatoFechaHora.format(new Date(instante));
}

/** Valor en pesos colombianos sin decimales, por ejemplo "$ 16.000". */
export function pesos(valor: number): string {
  return formatoPesos.format(valor);
}

/** Cantidad de puntos con la palabra en singular o plural: "1 punto", "8 puntos". */
export function textoPuntos(cantidad: number): string {
  return `${cantidad} ${Math.abs(cantidad) === 1 ? 'punto' : 'puntos'}`;
}

/** Fecha de hoy en Bogotá en formato AAAA-MM-DD (el que usa la API). */
export function hoyEnNegocio(ahora: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA_NEGOCIO }).format(ahora);
}
