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

/** Reserva tal como la ve el cliente que la hizo (ReservaResponse del backend). */
export interface Reserva {
  id: number;
  equipoId: number;
  nombreEquipo: string;
  inicio: string;
  fin: string;
  precioTotalPesos: number;
  adelantoPesos: number;
  estado: EstadoReserva;
  /** Límite para pagar el adelanto; null cuando ya no está PENDIENTE_PAGO. */
  expiraEn: string | null;
}

/** Equipo del catálogo (CatalogoResponse.EquipoCatalogo del backend). */
export interface EquipoCatalogo {
  id: number;
  nombre: string;
  modalidad: string;
  tarifaHoraPesos: number;
  estaDisponible: boolean;
}

/** Catálogo para reservar (CatalogoResponse del backend). */
export interface Catalogo {
  porcentajeAdelanto: number;
  minutosEsperaPago: number;
  equipos: EquipoCatalogo[];
}

/** Bloque de 30 minutos de un equipo (DisponibilidadResponse.Bloque del backend). */
export interface BloqueDisponibilidad {
  inicio: string;
  fin: string;
  estaLibre: boolean;
}

/** Bloques de un equipo en un día (DisponibilidadResponse del backend). */
export interface Disponibilidad {
  equipoId: number;
  nombreEquipo: string;
  fecha: string;
  bloques: BloqueDisponibilidad[];
}

/** Resultado que se le pide al proveedor de anticipos simulado (ResultadoSimulado del backend). */
export type ResultadoSimulado = 'APROBADO' | 'RECHAZADO';

/** CACS-3: una página del historial de reservas (HistorialReservasResponse del backend). */
export interface HistorialReservas {
  reservas: Reserva[];
  pagina: number;
  tamanoPagina: number;
  totalReservas: number;
  totalPaginas: number;
}

/** CACS-21: proveedor de pagos activo en el backend (app.pagos.proveedor). */
export type ProveedorPagos = 'SIMULADO' | 'WOMPI';

/** CACS-21: datos para ir al Web Checkout de Wompi (CheckoutWompiResponse del backend). */
export interface CheckoutWompi {
  pagoId: number;
  referencia: string;
  montoCentavos: number;
  moneda: string;
  llavePublica: string;
  firmaIntegridad: string;
  urlRetorno: string;
  urlCheckout: string;
}

/** Resultado de pagar el adelanto (PagoAnticipoResponse del backend). */
export interface PagoAnticipo {
  pagoId: number;
  referencia: string;
  montoCentavos: number;
  estadoPago: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'ANULADO' | 'ERROR';
  reservaId: number;
  estadoReserva: EstadoReserva;
  tieneAnticipoPagado: boolean;
  expiraEn: string | null;
}

/** Saldo e historial de puntos (SaldoPuntosResponse del backend). */
export interface SaldoPuntos {
  usuarioId: number;
  saldoPuntos: number;
  movimientos: MovimientoPuntos[];
}
