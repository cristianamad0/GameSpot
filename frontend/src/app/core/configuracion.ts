import { InjectionToken, isDevMode } from '@angular/core';

/**
 * URL base del backend. En desarrollo la API corre en el puerto 8080 y su CORS ya admite
 * http://localhost:4200. Las pruebas pueden reemplazarla con otro valor.
 */
export const URL_API = new InjectionToken<string>('URL_API', {
  factory: () => 'http://localhost:8080',
});

/**
 * CACS-17: si se muestra el selector "aprobar / rechazar" del proveedor de anticipos simulado.
 * Solo en el entorno de desarrollo y demostración (ng serve); en un build de producción no
 * aparece y el backend aprueba el pago simulado por defecto.
 */
export const PROVEEDOR_SIMULADO = new InjectionToken<boolean>('PROVEEDOR_SIMULADO', {
  factory: () => isDevMode(),
});

/**
 * CACS-21: lleva el navegador a otra página (el Web Checkout de Wompi). Las pruebas lo reemplazan
 * para no salir de la página.
 */
export const REDIRIGIR = new InjectionToken<(url: string) => void>('REDIRIGIR', {
  factory: () => (url: string) => window.location.assign(url),
});
