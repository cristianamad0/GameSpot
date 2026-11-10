import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { routes } from '../../app.routes';
import { sesionGuard } from '../../core/auth/guardas';
import { EstadoReserva, HistorialReservas, Reserva } from '../../core/modelos';
import { botonCon } from '../../pruebas/datos-admin';
import { MisReservas } from './mis-reservas';

const API = 'http://localhost:8080/api/reservas/mias';

function reserva(id: number, estado: EstadoReserva, numeroDia = 10): Reserva {
  const dia = String(numeroDia).padStart(2, '0');
  return {
    id,
    equipoId: 1,
    nombreEquipo: `Xbox 360 #${id}`,
    inicio: `2026-11-${dia}T20:00:00Z`,
    fin: `2026-11-${dia}T21:00:00Z`,
    precioTotalPesos: 5000,
    adelantoPesos: 2500,
    estado,
    expiraEn: estado === 'PENDIENTE_PAGO' ? `2026-11-${dia}T20:00:00Z` : null,
  };
}

function pagina(reservas: Reserva[], numero = 0, total = reservas.length): HistorialReservas {
  return {
    reservas,
    pagina: numero,
    tamanoPagina: 10,
    totalReservas: total,
    totalPaginas: Math.ceil(total / 10),
  };
}

describe('MisReservas (CACS-3)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<MisReservas>;
  let elemento: HTMLElement;

  async function crear(primera: HistorialReservas) {
    TestBed.configureTestingModule({
      imports: [MisReservas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MisReservas);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    responder(primera, (url) => url === `${API}?pagina=0`);
    await fixture.whenStable();
  }

  function responder(cuerpo: HistorialReservas, condicion: (url: string) => boolean) {
    const peticion = servidor.expectOne((r) => condicion(r.urlWithParams));
    peticion.flush(cuerpo);
    return peticion;
  }

  afterEach(() => servidor.verify());

  it('CP-F21a muestra cada reserva con equipo, horario, precio, adelanto y estado en texto', async () => {
    await crear(pagina([reserva(7, 'PENDIENTE_PAGO', 12), reserva(3, 'COMPLETADA', 2)]));

    const tarjetas = elemento.querySelectorAll('[data-reserva]');
    expect(tarjetas.length).toBe(2);
    expect(tarjetas[0].getAttribute('data-reserva')).toBe('7');
    expect(tarjetas[0].textContent).toContain('Xbox 360 #7');
    expect(tarjetas[0].textContent).toContain('Pendiente de pago');
    expect(tarjetas[0].textContent).toContain('5.000');
    expect(tarjetas[0].textContent).toContain('2.500');
    expect(tarjetas[1].textContent).toContain('Completada');
    expect(elemento.textContent).toContain('2 reservas');
  });

  it('CP-F21b una reserva pendiente lleva a pagar su adelanto; las demás no', async () => {
    await crear(pagina([reserva(7, 'PENDIENTE_PAGO'), reserva(3, 'CONFIRMADA', 2)]));

    const enlaces = elemento.querySelectorAll<HTMLAnchorElement>('a.reserva__pagar');
    expect(enlaces.length).toBe(1);
    expect(enlaces[0].getAttribute('href')).toBe('/reservas/7/pagar');
    expect(enlaces[0].getAttribute('aria-label')).toBe('Pagar el adelanto de Xbox 360 #7');
  });

  it('CP-F21c filtra por estado y vuelve a la primera página', async () => {
    await crear(pagina([reserva(7, 'PENDIENTE_PAGO')]));

    const select = elemento.querySelector<HTMLSelectElement>('#filtro-estado')!;
    select.value = 'CONFIRMADA';
    select.dispatchEvent(new Event('change'));
    const peticion = responder(pagina([reserva(3, 'CONFIRMADA')]), (url) =>
      url.includes('estado=CONFIRMADA'),
    );
    await fixture.whenStable();

    expect(peticion.request.params.get('pagina')).toBe('0');
    expect(elemento.querySelectorAll('[data-reserva]').length).toBe(1);
    expect(elemento.textContent).toContain('Confirmada');
  });

  it('CP-F21d pasa a la página siguiente y regresa', async () => {
    const primeras = Array.from({ length: 10 }, (_, i) => reserva(20 - i, 'COMPLETADA', 10));
    await crear(pagina(primeras, 0, 12));

    expect(elemento.textContent).toContain('página 1 de 2');
    expect(botonCon(elemento, 'Anterior').disabled).toBe(true);
    botonCon(elemento, 'Siguiente').click();
    responder(pagina([reserva(2, 'COMPLETADA'), reserva(1, 'CANCELADA')], 1, 12), (url) =>
      url.endsWith('pagina=1'),
    );
    await fixture.whenStable();

    expect(elemento.querySelectorAll('[data-reserva]').length).toBe(2);
    expect(botonCon(elemento, 'Siguiente').disabled).toBe(true);
    botonCon(elemento, 'Anterior').click();
    responder(pagina(primeras, 0, 12), (url) => url.endsWith('pagina=0'));
    await fixture.whenStable();

    expect(elemento.querySelectorAll('[data-reserva]').length).toBe(10);
  });

  it('CP-F21e sin reservas muestra un mensaje y un enlace para reservar', async () => {
    await crear(pagina([]));

    const vacio = elemento.querySelector('[role="status"]')!;
    expect(vacio.textContent).toContain('Aún no tienes reservas');
    expect(vacio.querySelector('a')?.getAttribute('href')).toBe('/reservar');
    expect(elemento.querySelector('nav.paginacion')).toBeNull();
  });

  it('CP-F21f un error al cargar se muestra con el mensaje del backend', async () => {
    TestBed.configureTestingModule({
      imports: [MisReservas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MisReservas);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    servidor
      .expectOne(`${API}?pagina=0`)
      .flush({ status: 400, mensaje: 'Datos invalidos' }, { status: 400, statusText: 'Bad' });
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('Datos invalidos');
  });

  it('CP-F21g la ruta /mis-reservas exige sesión', () => {
    const ruta = routes.find((r) => r.path === 'mis-reservas')!;
    expect(ruta.canActivate).toContain(sesionGuard);
  });
});
