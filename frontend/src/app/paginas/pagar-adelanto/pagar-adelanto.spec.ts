import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PROVEEDOR_SIMULADO } from '../../core/configuracion';
import { PagoAnticipo, Reserva } from '../../core/modelos';
import { botonCon } from '../../pruebas/datos-admin';
import { PagarAdelanto } from './pagar-adelanto';

const API = 'http://localhost:8080/api/reservas/3';

/** Reserva de Laura: 2 de marzo de 2099, 3:00 p. m. en Bogotá, con plazo de pago dentro de 15 minutos. */
function reservaPendiente(minutosParaPagar = 15): Reserva {
  return {
    id: 3,
    equipoId: 1,
    nombreEquipo: 'Xbox 360 #1',
    inicio: '2099-03-02T20:00:00Z',
    fin: '2099-03-02T21:00:00Z',
    precioTotalPesos: 5000,
    adelantoPesos: 2500,
    estado: 'PENDIENTE_PAGO',
    expiraEn: new Date(Date.now() + minutosParaPagar * 60_000).toISOString(),
  };
}

function pago(estadoPago: 'APROBADO' | 'RECHAZADO', expiraEn: string | null): PagoAnticipo {
  return {
    pagoId: 9,
    referencia: 'GS-3-ABC123',
    montoCentavos: 250_000,
    estadoPago,
    reservaId: 3,
    estadoReserva: estadoPago === 'APROBADO' ? 'CONFIRMADA' : 'PENDIENTE_PAGO',
    tieneAnticipoPagado: estadoPago === 'APROBADO',
    expiraEn,
  };
}

describe('PagarAdelanto (CACS-17, criterios 1 a 3)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<PagarAdelanto>;
  let elemento: HTMLElement;

  function configurar(esSimulado = true) {
    TestBed.configureTestingModule({
      imports: [PagarAdelanto],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PROVEEDOR_SIMULADO, useValue: esSimulado },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  }

  async function crear() {
    fixture = TestBed.createComponent(PagarAdelanto);
    fixture.componentRef.setInput('id', '3');
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  async function crearCon(reserva: Reserva) {
    await crear();
    servidor.expectOne(API).flush(reserva);
    await fixture.whenStable();
  }

  async function responderPago(cuerpo: object, opciones?: { status: number; statusText: string }) {
    const peticion = servidor.expectOne(`${API}/pago`);
    peticion.flush(cuerpo, opciones);
    await fixture.whenStable();
    return peticion;
  }

  function elegir(resultado: 'APROBADO' | 'RECHAZADO') {
    const radio = elemento.querySelector<HTMLInputElement>(`input[value="${resultado}"]`)!;
    radio.checked = true;
    radio.dispatchEvent(new Event('change'));
  }

  afterEach(() => servidor.verify());

  it('CP-F15a muestra monto, adelanto y plazo; al aprobarse el pago la reserva queda CONFIRMADA', async () => {
    configurar();
    await crearCon(reservaPendiente());

    expect(elemento.textContent).toContain('Xbox 360 #1');
    expect(elemento.textContent).toContain('Pendiente de pago');
    expect(elemento.querySelector('.montos__adelanto')?.textContent).toContain('2.500');
    expect(elemento.textContent?.replace(/\s/g, ' ')).toContain(
      'El resto, $ 2.500, se paga en el local.',
    );
    expect(elemento.querySelector('[role="timer"]')?.textContent).toMatch(/1[45] min \d\d s/);

    botonCon(elemento, 'Pagar').click();
    const peticion = await responderPago(pago('APROBADO', null));

    expect(peticion.request.body).toEqual({ resultadoSimulado: 'APROBADO' });
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Tu reserva quedó confirmada',
    );
    expect(elemento.textContent).toContain('Confirmada');
    expect(elemento.textContent).toContain('GS-3-ABC123');
    expect(elemento.querySelector('[role="timer"]')).toBeNull();
  });

  it('CP-F15b si el pago se rechaza, explica el motivo, sigue pendiente y se puede reintentar', async () => {
    configurar();
    const reserva = reservaPendiente();
    await crearCon(reserva);

    elegir('RECHAZADO');
    botonCon(elemento, 'Pagar').click();
    const rechazo = await responderPago(pago('RECHAZADO', reserva.expiraEn));

    expect(rechazo.request.body).toEqual({ resultadoSimulado: 'RECHAZADO' });
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'El proveedor de pagos rechazó el cobro',
    );
    expect(elemento.textContent).toContain('Pendiente de pago');

    elegir('APROBADO');
    botonCon(elemento, 'Pagar').click();
    await responderPago(pago('APROBADO', null));

    expect(elemento.textContent).toContain('Tu reserva quedó confirmada');
    expect(elemento.querySelector('[role="alert"]')).toBeNull();
  });

  it('CP-F15c con el plazo vencido no deja pagar y lo explica', async () => {
    configurar();
    await crearCon(reservaPendiente(-1));

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'El plazo para pagar venció',
    );
    expect(() => botonCon(elemento, 'Pagar')).toThrow();
  });

  it.each([
    [403, 'La reserva 3 no pertenece al usuario autenticado'],
    [404, 'No existe la reserva con id 3'],
  ])('CP-F15d si la reserva da %i, muestra el mensaje del backend', async (codigo, mensaje) => {
    configurar();
    await crear();
    servidor
      .expectOne(API)
      .flush({ status: codigo, mensaje }, { status: codigo, statusText: 'Error' });
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(mensaje);
    expect(elemento.textContent).toContain('Volver al inicio');
  });

  it('CP-F15e un 409 al pagar (ya pagada o vencida) se muestra con el mensaje del backend', async () => {
    configurar();
    await crearCon(reservaPendiente());

    botonCon(elemento, 'Pagar').click();
    await responderPago(
      { status: 409, mensaje: 'No se puede pagar la reserva 3 porque está en CONFIRMADA' },
      { status: 409, statusText: 'Conflict' },
    );

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'No se puede pagar la reserva 3 porque está en CONFIRMADA',
    );
  });

  it('CP-F15f sin el proveedor simulado no hay selector y el pago va sin resultado', async () => {
    configurar(false);
    await crearCon(reservaPendiente());

    expect(elemento.querySelector('fieldset')).toBeNull();
    botonCon(elemento, 'Pagar').click();
    const peticion = await responderPago(pago('APROBADO', null));

    expect(peticion.request.body).toEqual({});
  });

  it('CP-F15g una reserva ya confirmada no pide pago', async () => {
    configurar();
    await crearCon({ ...reservaPendiente(), estado: 'CONFIRMADA', expiraEn: null });

    expect(elemento.textContent).toContain('no tiene un adelanto pendiente de pago');
    expect(() => botonCon(elemento, 'Pagar')).toThrow();
  });
});
