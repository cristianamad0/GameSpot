import { InjectionToken } from '@angular/core';

/**
 * URL base del backend. En desarrollo la API corre en el puerto 8080 y su CORS ya admite
 * http://localhost:4200. Las pruebas pueden reemplazarla con otro valor.
 */
export const URL_API = new InjectionToken<string>('URL_API', {
  factory: () => 'http://localhost:8080',
});
