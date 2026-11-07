import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { hoyEnNegocio } from '../../core/formato';
import { BloqueDisponibilidad, Catalogo, Reserva } from '../../core/modelos';
import { botonCon, elegirOpcion } from '../../pruebas/datos-admin';
import {
  Reservar,
  adelantoReserva,
  iconoModalidad,
  precioReserva,
  textoDuracion,
} from './reservar';

const API = 'http://localhost:8080/api';

const catalogo: Catalogo = {
  porcentajeAdelanto: 50,
  minutosEsperaPago: 15,
  equipos: [
    {
      id: 1,
      nombre: 'Xbox 360 #1',
      modalidad: 'Xbox 360',
      tarifaHoraPesos: 5000,
      estaDisponible: true,
    },
    {
      id: 2,
      nombre: 'Xbox Series S #1',
      modalidad: 'Xbox Series S',
      tarifaHoraPesos: 6000,
      estaDisponible: false,
    },
    {
      id: 6,
      nombre: 'Sala VIP Xbox Series X',
      modalidad: 'Sala VIP Xbox Series X',
      tarifaHoraPesos: 7000,
      estaDisponible: true,
    },
  ],
};

/**
 * Lunes 2 de marzo de 2099 de 2:00 p. m. a 5:00 p. m. en Bogotá (19:00 a 22:00 UTC): seis
 * bloques; el de las 3:30 p. m. está ocupado.
 */
function bloquesDePrueba(): BloqueDisponibilidad[] {
  return [0, 1, 2, 3, 4, 5].map((n) => {
    const inicio = new Date(Date.UTC(2099, 2, 2, 19, 30 * n)).toISOString().replace('.000', '');
    const fin = new Date(Date.UTC(2099, 2, 2, 19, 30 * (n + 1))).toISOString().replace('.000', '');
    return { inicio, fin, estaLibre: n !== 3 };
  });
}

describe('Reservar (CACS-19, criterios 1 a 5)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<Reservar>;
  let elemento: HTMLElement;
  let router: Router;

  const bloque = (indice: number) =>
    elemento.querySelectorAll<HTMLButtonElement>('.bloques button')[indice];

  async function crear() {
    fixture = TestBed.createComponent(Reservar);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    servidor.expectOne(`${API}/catalogo`).flush(catalogo);
    await fixture.whenStable();
  }

  /** Elige el Xbox 360, cambia el día al 2 de marzo de 2099 y responde la disponibilidad. */
  async function verHorarios(bloques = bloquesDePrueba()) {
    elemento.querySelector<HTMLButtonElement>('[data-equipo="1"]')!.click();
    await fixture.whenStable();
    servidor
      .expectOne((r) => r.url === `${API}/reservas/disponibilidad`)
      .flush({ equipoId: 1, nombreEquipo: 'Xbox 360 #1', fecha: hoyEnNegocio(), bloques: [] });
    await fixture.whenStable();

    const fecha = elemento.querySelector<HTMLInputElement>('#fecha')!;
    fecha.value = '2099-03-02';
    fecha.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    const peticion = servidor.expectOne((r) => r.url === `${API}/reservas/disponibilidad`);
    expect(peticion.request.params.get('equipoId')).toBe('1');
    expect(peticion.request.params.get('fecha')).toBe('2099-03-02');
    peticion.flush({ equipoId: 1, nombreEquipo: 'Xbox 360 #1', fecha: '2099-03-02', bloques });
    await fixture.whenStable();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Reservar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => servidor.verify());

  it('CP-F18a el catálogo muestra cada equipo con su ícono y tarifa; el fuera de servicio no se elige', async () => {
    await crear();

    const xbox = elemento.querySelector<HTMLButtonElement>('[data-equipo="1"]')!;
    expect(xbox.textContent).toContain('Xbox 360 #1');
    expect(xbox.textContent?.replace(/\s/g, ' ')).toContain('$ 5.000 / hora');
    expect(xbox.textContent).toContain('🎮');
    expect(elemento.querySelector('[data-equipo="6"]')?.textContent).toContain('🛋️');

    const fuera = elemento.querySelector<HTMLButtonElement>('[data-equipo="2"]')!;
    expect(fuera.disabled).toBe(true);
    expect(fuera.textContent).toContain('Fuera de servicio');
  });

  it('CP-F18b muestra la disponibilidad del día con libres, ocupados y pasados en texto', async () => {
    await crear();
    await verHorarios();

    expect(elemento.querySelectorAll('.bloques button').length).toBe(6);
    expect(bloque(0).textContent).toContain('Libre');
    expect(bloque(3).textContent).toContain('Ocupado');
    expect(bloque(3).disabled).toBe(true);
  });

  it('CP-F18c los bloques que ya empezaron se ven como "Ya pasó" y no se eligen', async () => {
    await crear();
    const ayer = new Date(Date.now() - 24 * 3600_000).toISOString();
    await verHorarios([{ inicio: ayer, fin: ayer, estaLibre: true }]);

    expect(bloque(0).textContent).toContain('Ya pasó');
    expect(bloque(0).disabled).toBe(true);
  });

  it('CP-F18d solo deja duraciones de bloques libres seguidos y muestra precio y adelanto', async () => {
    await crear();
    await verHorarios();

    // 2:00 p. m.: hasta el bloque ocupado de las 3:30 caben 30, 60 o 90 minutos.
    bloque(0).click();
    await fixture.whenStable();
    const opciones = Array.from(
      elemento.querySelectorAll<HTMLOptionElement>('#duracion option'),
    ).map((o) => Number(o.value));
    expect(opciones).toEqual([30, 60, 90]);
    expect(bloque(0).getAttribute('aria-pressed')).toBe('true');
    expect(bloque(1).getAttribute('aria-pressed')).toBe('true');

    // Una hora del Xbox 360 a $5.000: precio $5.000 y adelanto del 50 %, $2.500.
    const detalle = elemento.querySelector('.detalle')!.textContent!.replace(/\s/g, ' ');
    expect(detalle).toContain('$ 5.000');
    expect(detalle).toContain('$ 2.500');

    elegirOpcion(elemento, '#duracion', 2);
    await fixture.whenStable();
    expect(elemento.querySelector('.detalle')!.textContent!.replace(/\s/g, ' ')).toContain(
      '$ 7.500',
    );
    expect(elemento.textContent).toContain('15 minutos para pagar el adelanto');
  });

  it('CP-F18e al reservar crea la reserva y lleva al cliente a pagar el adelanto', async () => {
    await crear();
    await verHorarios();
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    bloque(4).click();
    await fixture.whenStable();
    botonCon(elemento, 'Reservar y pagar').click();
    const peticion = servidor.expectOne(`${API}/reservas`);
    expect(peticion.request.method).toBe('POST');
    // 4:00 p. m. en Bogotá = 21:00 UTC; desde ahí solo caben 30 o 60 minutos, se elige 60.
    expect(peticion.request.body).toEqual({
      equipoId: 1,
      inicio: '2099-03-02T21:00:00Z',
      duracionMinutos: 60,
    });
    const creada: Reserva = {
      id: 7,
      equipoId: 1,
      nombreEquipo: 'Xbox 360 #1',
      inicio: '2099-03-02T21:00:00Z',
      fin: '2099-03-02T22:00:00Z',
      precioTotalPesos: 5000,
      adelantoPesos: 2500,
      estado: 'PENDIENTE_PAGO',
      expiraEn: '2099-03-01T15:15:00Z',
    };
    peticion.flush(creada, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(navegar).toHaveBeenCalledWith(['/reservas', 7, 'pagar']);
  });

  it('CP-F18f si el bloque se ocupó mientras elegía (409), lo avisa y actualiza la grilla', async () => {
    await crear();
    await verHorarios();

    bloque(0).click();
    await fixture.whenStable();
    botonCon(elemento, 'Reservar y pagar').click();
    servidor
      .expectOne(`${API}/reservas`)
      .flush(
        { status: 409, mensaje: 'El bloque ya está ocupado' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    const recarga = servidor.expectOne((r) => r.url === `${API}/reservas/disponibilidad`);
    const ocupado = bloquesDePrueba().map((b, n) => (n === 0 ? { ...b, estaLibre: false } : b));
    recarga.flush({
      equipoId: 1,
      nombreEquipo: 'Xbox 360 #1',
      fecha: '2099-03-02',
      bloques: ocupado,
    });
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'El bloque ya está ocupado',
    );
    expect(bloque(0).textContent).toContain('Ocupado');
    expect(bloque(0).disabled).toBe(true);
    expect(elemento.querySelector('#duracion')).toBeNull();

    // Al elegir otro bloque, el aviso del intento anterior desaparece.
    bloque(1).click();
    await fixture.whenStable();
    expect(elemento.querySelector('[role="alert"]')).toBeNull();
  });

  it('CP-F18g la grilla se usa con teclado: botones con nombre, estado y selección', async () => {
    await crear();
    await verHorarios();

    for (let i = 0; i < 6; i++) {
      expect(bloque(i).tagName).toBe('BUTTON');
      expect(bloque(i).getAttribute('aria-label')).toMatch(/ a .*(libre|ocupado)/);
    }
    expect(bloque(0).getAttribute('aria-label')).toMatch(
      /^2:00\s?p\.\s?m\. a 2:30\s?p\.\s?m\., libre$/,
    );

    // Enter o espacio sobre un botón disparan click: se simula el click del teclado.
    bloque(1).focus();
    bloque(1).click();
    await fixture.whenStable();
    expect(bloque(1).getAttribute('aria-label')).toContain('elegido');
    expect(elemento.querySelector('[role="group"]')?.getAttribute('aria-label')).toBe(
      'Horarios de Xbox 360 #1',
    );
  });

  it('CP-F18i un día sin atención lo explica', async () => {
    await crear();
    await verHorarios([]);

    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'El local no atiende ese día',
    );
  });

  it('CP-F18j el precio y el adelanto se calculan igual que en el backend', () => {
    expect(precioReserva(5000, 90)).toBe(7500);
    expect(precioReserva(7000, 30)).toBe(3500);
    expect(adelantoReserva(7500, 50)).toBe(3750);
    expect(adelantoReserva(3333, 50)).toBe(1667);
    expect(iconoModalidad('Sala VIP Xbox Series X')).toBe('🛋️');
    expect(iconoModalidad('Xbox Series S')).toBe('🎮');
    expect([30, 60, 90, 120].map(textoDuracion)).toEqual([
      '30 minutos',
      '1 hora',
      '1 hora 30 minutos',
      '2 horas',
    ]);
  });
});
