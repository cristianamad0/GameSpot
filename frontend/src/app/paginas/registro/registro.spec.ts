import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Sesion } from '../../core/auth/sesion';
import { respuestaDePrueba } from '../../pruebas/utilidades';
import { Registro } from './registro';

const URL_REGISTRO = 'http://localhost:8080/api/auth/registro';

describe('Registro (CACS-15, criterios 1 y 4)', () => {
  let servidor: HttpTestingController;

  async function llenar(elemento: HTMLElement, datos: Record<string, string>) {
    for (const [id, valor] of Object.entries(datos)) {
      const input = elemento.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
    }
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('CP-F06a registra al cliente, abre su sesión y vuelve al inicio', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Registro);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;

    await llenar(elemento, {
      nombre: '  Cliente Demo ',
      email: 'cliente@gamespot.test',
      password: 'clave-segura',
      confirmacion: 'clave-segura',
    });
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));

    const peticion = servidor.expectOne(URL_REGISTRO);
    expect(peticion.request.body).toEqual({
      nombre: 'Cliente Demo',
      email: 'cliente@gamespot.test',
      password: 'clave-segura',
    });
    peticion.flush(respuestaDePrueba('CLIENTE'));

    expect(TestBed.inject(Sesion).estaAutenticado()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/');
  });

  it('CP-F06b no envía si la contraseña es corta o no coincide', async () => {
    const fixture = TestBed.createComponent(Registro);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;

    await llenar(elemento, {
      nombre: 'Cliente Demo',
      email: 'cliente@gamespot.test',
      password: 'corta',
      confirmacion: 'distinta',
    });
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    servidor.expectNone(URL_REGISTRO);
    expect(elemento.textContent).toContain('Usa al menos 8 caracteres.');
    expect(elemento.textContent).toContain('Las contraseñas no coinciden.');
  });

  it('CP-F06d con la confirmación vacía pide repetir la contraseña', async () => {
    const fixture = TestBed.createComponent(Registro);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;

    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    servidor.expectNone(URL_REGISTRO);
    expect(elemento.querySelector('#confirmacion-error')?.textContent).toContain(
      'Repite tu contraseña.',
    );
  });

  it('CP-F06c un correo ya registrado muestra el mensaje del backend (409)', async () => {
    const fixture = TestBed.createComponent(Registro);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;

    await llenar(elemento, {
      nombre: 'Cliente Demo',
      email: 'cliente@gamespot.test',
      password: 'clave-segura',
      confirmacion: 'clave-segura',
    });
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    servidor
      .expectOne(URL_REGISTRO)
      .flush(
        { status: 409, mensaje: 'El correo ya está registrado', detalles: [] },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'El correo ya está registrado',
    );
  });
});
