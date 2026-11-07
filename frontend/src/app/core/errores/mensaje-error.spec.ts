import { HttpErrorResponse } from '@angular/common/http';
import { mensajeDeError } from './mensaje-error';

describe('mensajeDeError (CACS-15, criterio 4)', () => {
  it('CP-F04a usa el mensaje del ApiError del backend', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { status: 409, mensaje: 'El bloque ya está ocupado', detalles: [] },
    });

    expect(mensajeDeError(error)).toBe('El bloque ya está ocupado');
  });

  it('CP-F04b agrega los detalles de validación', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        mensaje: 'Datos invalidos',
        detalles: ['password: La contraseña debe tener al menos 8 caracteres'],
      },
    });

    expect(mensajeDeError(error)).toBe(
      'Datos invalidos: password: La contraseña debe tener al menos 8 caracteres',
    );
  });

  it('CP-F04c explica la falta de conexión y los errores sin cuerpo', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toContain(
      'No hay conexión con el servidor',
    );
    expect(mensajeDeError(new HttpErrorResponse({ status: 403 }))).toBe(
      'No tienes permiso para hacer esto.',
    );
    expect(mensajeDeError(new HttpErrorResponse({ status: 500 }))).toContain('El servidor');
    expect(mensajeDeError(new Error('x'))).toContain('error inesperado');
  });
});
