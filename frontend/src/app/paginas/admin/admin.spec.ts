import { routes } from '../../app.routes';
import { adminGuard } from '../../core/auth/guardas';

describe('Rutas del panel (CACS-18, criterio 6)', () => {
  it('CP-F13 todas las pantallas del panel cuelgan de /admin, protegido por adminGuard', () => {
    const admin = routes.find((ruta) => ruta.path === 'admin')!;

    expect(admin.canActivate).toContain(adminGuard);
    expect(admin.children?.map((hija) => hija.path)).toEqual([
      '',
      'expiradas',
      'puntos',
      'equipos',
    ]);
    // Ninguna hija vuelve a definir canActivate: la guarda del padre aplica a todas.
    expect(admin.children?.every((hija) => !hija.canActivate)).toBe(true);
  });
});
