import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SaldoPuntos } from '../../core/modelos';
import { MisPuntos } from './mis-puntos';

const API = 'http://localhost:8080/api/puntos';

describe('MisPuntos (CACS-17, criterio 4)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<MisPuntos>;
  let elemento: HTMLElement;

  async function crear(respuesta: SaldoPuntos) {
    fixture = TestBed.createComponent(MisPuntos);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    servidor.expectOne(API).flush(respuesta);
    await fixture.whenStable();
  }

  const idsVisibles = () =>
    Array.from(elemento.querySelectorAll<HTMLElement>('[data-movimiento]')).map((li) =>
      Number(li.dataset['movimiento']),
    );

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MisPuntos],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('CP-F16a muestra el saldo y el historial del más reciente al más antiguo', async () => {
    // Llegan desordenados a propósito: la pantalla los ordena por fecha.
    await crear({
      usuarioId: 2,
      saldoPuntos: 9,
      movimientos: [
        {
          id: 1,
          fecha: '2026-11-04T21:00:00Z',
          puntos: 2,
          tipo: 'HORAS_JUGADAS',
          descripcion: '120 minutos jugados',
          reservaId: 7,
        },
        {
          id: 3,
          fecha: '2026-11-06T15:00:00Z',
          puntos: -3,
          tipo: 'AJUSTE_ADMIN',
          descripcion: 'Canje por una bebida',
          reservaId: null,
        },
        {
          id: 2,
          fecha: '2026-11-05T15:00:00Z',
          puntos: 10,
          tipo: 'AJUSTE_ADMIN',
          descripcion: 'Bono de bienvenida',
          reservaId: null,
        },
      ],
    });

    expect(elemento.querySelector('.saldo__numero')?.textContent).toBe('9');
    expect(elemento.querySelector('.saldo__valor')?.textContent).toContain('puntos');
    expect(idsVisibles()).toEqual([3, 2, 1]);

    const primero = elemento.querySelector<HTMLElement>('[data-movimiento="3"]')!;
    expect(primero.textContent).toContain('Ajuste del administrador');
    expect(primero.textContent).toContain('Canje por una bebida');
    expect(primero.textContent).toContain('−3 puntos');
    const ultimo = elemento.querySelector<HTMLElement>('[data-movimiento="1"]')!;
    expect(ultimo.textContent).toContain('Horas jugadas');
    expect(ultimo.textContent).toContain('+2 puntos');
  });

  it('CP-F16b sin movimientos muestra un mensaje amigable', async () => {
    await crear({ usuarioId: 2, saldoPuntos: 0, movimientos: [] });

    expect(elemento.querySelector('.saldo__numero')?.textContent).toBe('0');
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Aún no tienes movimientos',
    );
    expect(idsVisibles()).toEqual([]);
  });

  it('CP-F16c si la API falla, muestra el error', async () => {
    fixture = TestBed.createComponent(MisPuntos);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    servidor.expectOne(API).flush(null, { status: 0, statusText: 'Unknown Error' });
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'No hay conexión con el servidor',
    );
  });
});
