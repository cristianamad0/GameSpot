import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from '../modelos';

/**
 * CACS-15: convierte un error HTTP en un mensaje en español para mostrar en pantalla. Usa el
 * mensaje de ApiError cuando el backend lo envía; si no, uno genérico según el código.
 */
export function mensajeDeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado. Intenta de nuevo.';
  }
  if (error.status === 0) {
    return 'No hay conexión con el servidor. Revisa que la API esté encendida e intenta de nuevo.';
  }

  const cuerpo = error.error as Partial<ErrorApi> | null;
  if (cuerpo && typeof cuerpo.mensaje === 'string' && cuerpo.mensaje.trim()) {
    const detalles = (cuerpo.detalles ?? []).filter((detalle) => detalle?.trim());
    return detalles.length ? `${cuerpo.mensaje}: ${detalles.join('; ')}` : cuerpo.mensaje;
  }

  switch (error.status) {
    case 400:
      return 'Revisa los datos ingresados.';
    case 401:
      return 'Tu sesión no es válida. Inicia sesión de nuevo.';
    case 403:
      return 'No tienes permiso para hacer esto.';
    case 404:
      return 'No encontramos lo que buscas.';
    case 409:
      return 'No se pudo completar porque el estado cambió. Actualiza e intenta de nuevo.';
    default:
      return 'El servidor tuvo un problema. Intenta de nuevo en unos minutos.';
  }
}
