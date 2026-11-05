/** Rol del usuario, igual que el enum Rol del backend. */
export type Rol = 'CLIENTE' | 'ADMIN';

/** Respuesta de /api/auth/login y /api/auth/registro (AuthResponse del backend). */
export interface RespuestaAutenticacion {
  token: string;
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
}

/** Usuario con sesión abierta en el navegador. */
export interface UsuarioSesion {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
}

/** Cuerpo de los errores de la API (ApiError del backend). */
export interface ErrorApi {
  timestamp?: string;
  status: number;
  mensaje: string;
  detalles?: string[];
}

/** Estados de una reserva, iguales a EstadoReserva del backend. */
export type EstadoReserva =
  'PENDIENTE_PAGO' | 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA' | 'EXPIRADA';

/** Todos los estados, en el orden del ciclo de vida. */
export const ESTADOS_RESERVA: EstadoReserva[] = [
  'PENDIENTE_PAGO',
  'CONFIRMADA',
  'COMPLETADA',
  'CANCELADA',
  'EXPIRADA',
];

/** Reserva tal como la ve el administrador (ReservaAdminResponse del backend). */
export interface ReservaAdmin {
  id: number;
  usuarioId: number;
  nombreCliente: string;
  emailCliente: string;
  equipoId: number;
  nombreEquipo: string;
  inicio: string;
  fin: string;
  precioTotalPesos: number;
  adelantoPesos: number;
  estado: EstadoReserva;
  tieneAnticipoPagado: boolean;
  canceladaEn: string | null;
  motivoCancelacion: string | null;
}

/** Reserva que expiró con un pago aprobado (ReservaExpiradaConPagoResponse del backend). */
export interface ReservaExpiradaConPago {
  reserva: ReservaAdmin;
  referenciaPago: string;
  montoPagadoCentavos: number;
  pagoAprobadoEn: string;
}

/** Usuario encontrado por el administrador (UsuarioResumenResponse del backend). */
export interface UsuarioResumen {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
  puntosFidelidad: number;
}

/** Movimiento del historial de puntos (MovimientoPuntosResponse del backend). */
export interface MovimientoPuntos {
  id: number;
  fecha: string;
  puntos: number;
  tipo: 'HORAS_JUGADAS' | 'AJUSTE_ADMIN';
  descripcion: string | null;
  reservaId: number | null;
}

/** Saldo e historial de puntos (SaldoPuntosResponse del backend). */
export interface SaldoPuntos {
  usuarioId: number;
  saldoPuntos: number;
  movimientos: MovimientoPuntos[];
}
