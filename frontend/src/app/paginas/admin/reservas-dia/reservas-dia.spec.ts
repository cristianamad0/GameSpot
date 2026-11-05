import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { hoyEnNegocio } from '../../../core/formato';
import { ReservaAdmin } from '../../../core/modelos';
import { botonCon, elegirOpcion, escribir, reservaDePrueba } from '../../../pruebas/datos-admin';
import { ReservasDia } from './reservas-dia';

const API = 'http://localhost:8080/api/admin/reservas';

describe('ReservasDia (CACS-18, criterios 1 a 3)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<ReservasDia>;
  let elemento: HTMLElement;

  // Xbox 360: 20:00 UTC = 3:00 p. m. en Bogotá; Series S: dos reservas, una a las 4 p. m.
  const reservas: ReservaAdmin[] = [
    reservaDePrueba(1, 1, 'Xbox 360 #1', '20:00', 'CONFIRMADA'),
    reservaDePrueba(2, 2, 'Xbox Series S #1', '21:00', 'PENDIENTE_PAGO'),
    reservaDePrueba(3, 2, 'Xbox Series S #1', '19:00', 'COMPLETADA'),
  ];

  async function crear(respuesta: ReservaAdmin[] = reservas) {
    fixture = TestBed.createComponent(ReservasDia);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    servidor
      .expectOne((r) => r.url === API && r.params.get('fecha') === hoyEnNegocio())
      .flush(respuesta);
    await fixture.whenStable();
  }

  const tarjeta = (id: number) => elemento.querySelector<HTMLElement>(`[data-reserva="${id}"]`)!;
  const idsVisibles = () =>
    Array.from(elemento.querySelectorAll<HTMLElement>('[data-reserva]')).map((li) =>
      Number(li.dataset['reserva']),
    );

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReservasDia],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('CP-F10a muestra las reservas de hoy agrupadas por equipo y en orden de hora', async () => {
    await crear();

    const equipos = Array.from(elemento.querySelectorAll('h2')).map((h) => h.textContent?.trim());
    expect(equipos).toEqual(['Xbox Series S #1', 'Xbox 360 #1']);
    expect(idsVisibles()).toEqual([3, 2, 1]);
    expect(tarjeta(1).textContent).toContain('Cliente 1');
    expect(tarjeta(1).textContent).toContain('Confirmada');
    expect(elemento.textContent).toContain('3 reservas');
  });

  it('CP-F10b filtra por equipo y por estado', async () => {
    await crear();

    elegirOpcion(elemento, '#filtro-equipo', 2); // [Todos, Xbox 360, Series S]
    await fixture.whenStable();
    expect(idsVisibles()).toEqual([3, 2]);

    elegirOpcion(elemento, '#filtro-estado', 1); // [Todos, PENDIENTE_PAGO, ...]
    await fixture.whenStable();
    expect(idsVisibles()).toEqual([2]);

    elegirOpcion(elemento, '#filtro-equipo', 1);
    await fixture.whenStable();
    expect(idsVisibles()).toEqual([]);
    expect(elemento.textContent).toContain('No hay reservas para este día');
  });

  it('CP-F10c cancelar pide motivo y actualiza la reserva sin recargar', async () => {
    await crear();

    botonCon(tarjeta(1), 'Cancelar').click();
    await fixture.whenStable();
    expect(botonCon(tarjeta(1), 'Confirmar cancelación').disabled).toBe(true);

    escribir(tarjeta(1), 'textarea', '  Falla eléctrica  ');
    await fixture.whenStable();
    botonCon(tarjeta(1), 'Confirmar cancelación').click();

    const peticion = servidor.expectOne(`${API}/1/cancelar`);
    expect(peticion.request.body).toEqual({ motivo: 'Falla eléctrica' });
    peticion.flush({
      ...reservas[0],
      estado: 'CANCELADA',
      canceladaEn: '2026-11-10T18:00:00Z',
      motivoCancelacion: 'Falla eléctrica',
    });
    await fixture.whenStable();

    expect(tarjeta(1).textContent).toContain('Cancelada');
    expect(tarjeta(1).textContent).toContain('Motivo: Falla eléctrica');
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain('cancelada');
    servidor.expectNone((r) => r.method === 'GET');
  });

  it('CP-F10d completar una reserva confirmada la muestra como completada', async () => {
    await crear();

    botonCon(tarjeta(1), 'Completar').click();
    await fixture.whenStable();
    botonCon(tarjeta(1), 'Sí, completar').click();
    servidor.expectOne(`${API}/1/completar`).flush({ ...reservas[0], estado: 'COMPLETADA' });
    await fixture.whenStable();

    expect(tarjeta(1).textContent).toContain('Completada');
    expect(tarjeta(1).textContent).not.toContain('Cancelar');
  });

  it('CP-F10e solo ofrece las acciones que el ciclo de vida permite', async () => {
    await crear();

    expect(tarjeta(1).textContent).toContain('Completar'); // CONFIRMADA
    expect(tarjeta(2).textContent).not.toContain('Completar'); // PENDIENTE_PAGO
    expect(tarjeta(2).textContent).toContain('Cancelar');
    expect(tarjeta(3).querySelectorAll('button').length).toBe(0); // COMPLETADA
  });

  it('CP-F10f un 409 muestra el mensaje del backend y no cambia la reserva', async () => {
    await crear();

    botonCon(tarjeta(1), 'Completar').click();
    await fixture.whenStable();
    botonCon(tarjeta(1), 'Sí, completar').click();
    servidor
      .expectOne(`${API}/1/completar`)
      .flush(
        { status: 409, mensaje: 'No se puede completar la reserva 1 porque está en CANCELADA' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(tarjeta(1).querySelector('[role="alert"]')?.textContent).toContain(
      'porque está en CANCELADA',
    );
    expect(tarjeta(1).textContent).toContain('Confirmada');
  });

  it('CP-F10g al cambiar el día vuelve a consultar con la nueva fecha', async () => {
    await crear();

    escribir(elemento, '#fecha', '2026-11-12');
    await fixture.whenStable();

    servidor.expectOne((r) => r.url === API && r.params.get('fecha') === '2026-11-12').flush([]);
    await fixture.whenStable();
    expect(elemento.textContent).toContain('0 reservas');
  });
});
