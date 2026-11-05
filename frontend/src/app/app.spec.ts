import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { Sesion } from './core/auth/sesion';
import { respuestaDePrueba } from './pruebas/utilidades';

describe('App y barra de navegación (CACS-15, criterios 1 y 3)', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([])] });
  });

  async function enlaces(): Promise<string[]> {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const nav = (fixture.nativeElement as HTMLElement).querySelector('nav')!;
    return Array.from(nav.querySelectorAll('a')).map((a) => a.textContent!.trim());
  }

  it('CP-F08a sin sesión ofrece ingresar y crear cuenta', async () => {
    expect(await enlaces()).toEqual(['Inicio', 'Ingresar', 'Crear cuenta']);
  });

  it('CP-F08b un CLIENTE ve reservar y mis puntos, pero no el panel', async () => {
    TestBed.inject(Sesion).abrir(respuestaDePrueba('CLIENTE'));

    expect(await enlaces()).toEqual(['Inicio', 'Reservar', 'Mis puntos']);
  });

  it('CP-F08c un ADMIN ve el panel', async () => {
    TestBed.inject(Sesion).abrir(respuestaDePrueba('ADMIN'));

    expect(await enlaces()).toEqual(['Inicio', 'Panel']);
  });
});
