import { TestBed } from '@angular/core/testing';
import { respuestaDePrueba, tokenDePrueba } from '../../pruebas/utilidades';
import { CLAVE_SESION, Sesion, tokenVencido } from './sesion';

describe('Sesion (CACS-15, criterio 1)', () => {
  beforeEach(() => localStorage.clear());

  it('CP-F01a abre la sesión, la guarda y la recupera al recargar', () => {
    const sesion = TestBed.inject(Sesion);
    const respuesta = respuestaDePrueba('CLIENTE');

    sesion.abrir(respuesta);

    expect(sesion.estaAutenticado()).toBe(true);
    expect(sesion.usuario()?.nombre).toBe('Cliente Demo');
    expect(sesion.token()).toBe(respuesta.token);
    expect(JSON.parse(localStorage.getItem(CLAVE_SESION)!).token).toBe(respuesta.token);

    TestBed.resetTestingModule();
    const recargada = TestBed.inject(Sesion);
    expect(recargada.usuario()?.email).toBe('cliente@gamespot.test');
  });

  it('CP-F01b cerrar la sesión borra el token guardado', () => {
    const sesion = TestBed.inject(Sesion);
    sesion.abrir(respuestaDePrueba());

    sesion.cerrar();

    expect(sesion.estaAutenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('CP-F01c un token vencido cierra la sesión', () => {
    const sesion = TestBed.inject(Sesion);
    sesion.abrir(respuestaDePrueba('CLIENTE', -60));

    expect(sesion.token()).toBeNull();
    expect(sesion.estaAutenticado()).toBe(false);
  });

  it('CP-F01d tokenVencido lee el campo exp y trata un token ilegible como vencido', () => {
    expect(tokenVencido(tokenDePrueba(60))).toBe(false);
    expect(tokenVencido(tokenDePrueba(-1))).toBe(true);
    expect(tokenVencido('no-es-un-jwt')).toBe(true);
  });
});
