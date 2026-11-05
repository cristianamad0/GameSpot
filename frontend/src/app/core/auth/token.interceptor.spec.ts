import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { respuestaDePrueba } from '../../pruebas/utilidades';
import { Sesion } from './sesion';
import { tokenInterceptor } from './token.interceptor';

const API = 'http://localhost:8080';

describe('tokenInterceptor (CACS-15, criterio 2)', () => {
  let http: HttpClient;
  let servidor: HttpTestingController;
  let sesion: Sesion;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    servidor = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(Sesion);
    router = TestBed.inject(Router);
  });

  afterEach(() => servidor.verify());

  it('CP-F02a agrega el token a las peticiones a la API', () => {
    const respuesta = respuestaDePrueba();
    sesion.abrir(respuesta);

    http.get(`${API}/api/puntos`).subscribe();

    const peticion = servidor.expectOne(`${API}/api/puntos`);
    expect(peticion.request.headers.get('Authorization')).toBe(`Bearer ${respuesta.token}`);
    peticion.flush({});
  });

  it('CP-F02b no envía el token a otros sitios ni a /api/auth', () => {
    sesion.abrir(respuestaDePrueba());

    http.get('https://fonts.googleapis.com/css').subscribe();
    http.post(`${API}/api/auth/login`, {}).subscribe();

    expect(
      servidor.expectOne('https://fonts.googleapis.com/css').request.headers.has('Authorization'),
    ).toBe(false);
    expect(servidor.expectOne(`${API}/api/auth/login`).request.headers.has('Authorization')).toBe(
      false,
    );
  });

  it('CP-F02c un 401 de la API cierra la sesión y lleva al inicio de sesión', () => {
    sesion.abrir(respuestaDePrueba());
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    http.get(`${API}/api/puntos`).subscribe({ error: () => undefined });
    servidor
      .expectOne(`${API}/api/puntos`)
      .flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(sesion.estaAutenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/ingresar'], {
      queryParams: { vuelta: '/', motivo: 'sesion-vencida' },
    });
  });

  it('CP-F02d un 401 del login no cierra la sesión ni redirige', () => {
    const navegar = vi.spyOn(router, 'navigate');

    http.post(`${API}/api/auth/login`, {}).subscribe({ error: () => undefined });
    servidor
      .expectOne(`${API}/api/auth/login`)
      .flush(
        { status: 401, mensaje: 'Correo o contraseña incorrectos' },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(navegar).not.toHaveBeenCalled();
  });
});
