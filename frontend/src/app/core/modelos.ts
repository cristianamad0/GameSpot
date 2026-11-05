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
