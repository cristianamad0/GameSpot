import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { pesos } from '../../../core/formato';
import { reservaDePrueba } from '../../../pruebas/datos-admin';
import { Expiradas } from './expiradas';

const API = 'http://localhost:8080/api/admin/reservas/expiradas-con-pago';

describe('Expiradas (CACS-18, criterio 4)', () => {
  let servidor: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Expiradas],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('CP-F11a muestra las reservas expiradas con su pago aprobado', async () => {
    const fixture = TestBed.createComponent(Expiradas);
    await fixture.whenStable();
    servidor.expectOne(API).flush([
      {
        reserva: reservaDePrueba(7, 1, 'Xbox 360 #1', '20:00', 'EXPIRADA'),
        referenciaPago: 'GS-7-ABC123',
        montoPagadoCentavos: 400000,
        pagoAprobadoEn: '2026-11-10T19:50:00Z',
      },
    ]);
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texto).toContain('Xbox 360 #1');
    expect(texto).toContain('Cliente 7');
    expect(texto).toContain('Expirada');
    expect(texto).toContain('GS-7-ABC123');
    expect(texto).toContain(pesos(4000));
  });

  it('CP-F11b sin casos muestra que todo está al día', async () => {
    const fixture = TestBed.createComponent(Expiradas);
    await fixture.whenStable();
    servidor.expectOne(API).flush([]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay casos pendientes');
  });
});
