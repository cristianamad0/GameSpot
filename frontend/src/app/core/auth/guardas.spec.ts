import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { respuestaDePrueba } from '../../pruebas/utilidades';
import { adminGuard, sesionGuard } from './guardas';
import { Sesion } from './sesion';

describe('Guardas de ruta (CACS-15, criterio 3)', () => {
  let sesion: Sesion;
  let router: Router;
  const estado = (url: string) => ({ url }) as RouterStateSnapshot;
  const ruta = {} as ActivatedRouteSnapshot;
  const ejecutar = (guarda: typeof sesionGuard, url: string) =>
    TestBed.runInInjectionContext(() => guarda(ruta, estado(url)));

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    sesion = TestBed.inject(Sesion);
    router = TestBed.inject(Router);
  });

  it('CP-F03a sin sesión, una ruta protegida lleva al inicio de sesión recordando la vuelta', () => {
    const resultado = ejecutar(sesionGuard, '/mis-puntos') as UrlTree;

    expect(router.serializeUrl(resultado)).toBe('/ingresar?vuelta=%2Fmis-puntos');
  });

  it('CP-F03b con sesión de CLIENTE puede entrar a sus rutas', () => {
    sesion.abrir(respuestaDePrueba('CLIENTE'));

    expect(ejecutar(sesionGuard, '/mis-puntos')).toBe(true);
  });

  it('CP-F03c un CLIENTE no puede entrar a /admin: vuelve al inicio', () => {
    sesion.abrir(respuestaDePrueba('CLIENTE'));

    const resultado = ejecutar(adminGuard, '/admin') as UrlTree;

    expect(router.serializeUrl(resultado)).toBe('/');
  });

  it('CP-F03d un ADMIN sí entra a /admin y sin sesión se pide iniciar sesión', () => {
    const sinSesion = ejecutar(adminGuard, '/admin') as UrlTree;
    expect(router.serializeUrl(sinSesion)).toBe('/ingresar?vuelta=%2Fadmin');

    sesion.abrir(respuestaDePrueba('ADMIN'));
    expect(ejecutar(adminGuard, '/admin')).toBe(true);
  });
});
