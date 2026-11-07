import { Injectable, computed, signal } from '@angular/core';
import { RespuestaAutenticacion, UsuarioSesion } from '../modelos';

/** Clave con la que se guarda la sesión en localStorage. */
export const CLAVE_SESION = 'gamespot.sesion';

interface SesionGuardada {
  token: string;
  usuario: UsuarioSesion;
}

/**
 * CACS-15: guarda el token JWT y los datos del usuario mientras la sesión esté abierta. La API es
 * sin estado: el token viaja en cada petición (ver tokenInterceptor) y vence en una hora.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly sesion = signal<SesionGuardada | null>(leerSesionGuardada());

  /** Usuario con sesión abierta, o null. */
  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly estaAutenticado = computed(() => this.sesion() !== null);
  readonly esAdmin = computed(() => this.usuario()?.rol === 'ADMIN');

  /** Token vigente, o null si no hay sesión o ya venció (en ese caso la cierra). */
  token(): string | null {
    const actual = this.sesion();
    if (!actual) {
      return null;
    }
    if (tokenVencido(actual.token)) {
      this.cerrar();
      return null;
    }
    return actual.token;
  }

  /** Abre la sesión con la respuesta del login o del registro. */
  abrir(respuesta: RespuestaAutenticacion): void {
    const sesion: SesionGuardada = {
      token: respuesta.token,
      usuario: {
        id: respuesta.id,
        nombre: respuesta.nombre,
        email: respuesta.email,
        rol: respuesta.rol,
      },
    };
    this.sesion.set(sesion);
    try {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      // Sin almacenamiento (modo privado): la sesión dura mientras la pestaña esté abierta.
    }
  }

  cerrar(): void {
    this.sesion.set(null);
    try {
      localStorage.removeItem(CLAVE_SESION);
    } catch {
      // Nada que borrar.
    }
  }
}

function leerSesionGuardada(): SesionGuardada | null {
  try {
    const texto = localStorage.getItem(CLAVE_SESION);
    if (!texto) {
      return null;
    }
    const sesion = JSON.parse(texto) as SesionGuardada;
    if (!sesion?.token || !sesion.usuario || tokenVencido(sesion.token)) {
      localStorage.removeItem(CLAVE_SESION);
      return null;
    }
    return sesion;
  } catch {
    return null;
  }
}

/** Lee el campo exp del JWT. Un token ilegible se trata como vencido. */
export function tokenVencido(token: string, ahoraMs: number = Date.now()): boolean {
  try {
    const carga = token.split('.')[1];
    const json = atob(carga.replace(/-/g, '+').replace(/_/g, '/'));
    const exp = (JSON.parse(json) as { exp?: number }).exp;
    return typeof exp !== 'number' || exp * 1000 <= ahoraMs;
  } catch {
    return true;
  }
}
