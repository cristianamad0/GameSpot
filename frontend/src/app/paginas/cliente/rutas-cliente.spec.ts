import { routes } from '../../app.routes';
import { sesionGuard } from '../../core/auth/guardas';

describe('Rutas del cliente (CACS-17 y CACS-19)', () => {
  it('CP-F18h reservar exige sesión y ya no es "Próximamente"', () => {
    const ruta = routes.find((r) => r.path === 'reservar')!;
    expect(ruta.canActivate).toContain(sesionGuard);
    expect(ruta.data).toBeUndefined();
  });

  it('CP-F17 mis puntos y pagar el adelanto exigen sesión y ya no son "Próximamente"', () => {
    for (const path of ['mis-puntos', 'reservas/:id/pagar']) {
      const ruta = routes.find((r) => r.path === path)!;
      expect(ruta.canActivate).toContain(sesionGuard);
      expect(ruta.data).toBeUndefined();
    }
  });
});
