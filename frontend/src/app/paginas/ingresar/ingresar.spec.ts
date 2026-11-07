import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Sesion } from '../../core/auth/sesion';
import { respuestaDePrueba } from '../../pruebas/utilidades';
import { Ingresar } from './ingresar';

const URL_LOGIN = 'http://localhost:8080/api/auth/login';

describe('Ingresar (CACS-15, criterios 1 y 4)', () => {
  let servidor: HttpTestingController;
  let router: Router;

  async function crear() {
    const fixture = TestBed.createComponent(Ingresar);
    await fixture.whenStable();
    return fixture;
  }

  async function escribir(elemento: HTMLElement, selector: string, valor: string) {
    const input = elemento.querySelector<HTMLInputElement>(selector)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Ingresar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => servidor.verify());

  it('CP-F05a con datos válidos inicia sesión y lleva al administrador a su panel', async () => {
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = await crear();
    const elemento = fixture.nativeElement as HTMLElement;

    await escribir(elemento, '#email', 'admin@gamespot.test');
    await escribir(elemento, '#password', 'clave-de-prueba');
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));

    const peticion = servidor.expectOne(URL_LOGIN);
    expect(peticion.request.body).toEqual({
      email: 'admin@gamespot.test',
      password: 'clave-de-prueba',
    });
    peticion.flush(respuestaDePrueba('ADMIN'));

    expect(TestBed.inject(Sesion).esAdmin()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/admin');
  });

  it('CP-F05b con credenciales incorrectas muestra el mensaje del backend', async () => {
    const fixture = await crear();
    const elemento = fixture.nativeElement as HTMLElement;

    await escribir(elemento, '#email', 'cliente@gamespot.test');
    await escribir(elemento, '#password', 'equivocada');
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    servidor
      .expectOne(URL_LOGIN)
      .flush(
        { status: 401, mensaje: 'Correo o contraseña incorrectos', detalles: [] },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Correo o contraseña incorrectos',
    );
    expect(TestBed.inject(Sesion).estaAutenticado()).toBe(false);
  });

  it('CP-F05c con el formulario vacío no llama a la API y marca los campos', async () => {
    const fixture = await crear();
    const elemento = fixture.nativeElement as HTMLElement;

    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    servidor.expectNone(URL_LOGIN);
    expect(elemento.querySelector('#email')?.getAttribute('aria-invalid')).toBe('true');
    expect(elemento.textContent).toContain('Escribe un correo válido.');
  });
});
