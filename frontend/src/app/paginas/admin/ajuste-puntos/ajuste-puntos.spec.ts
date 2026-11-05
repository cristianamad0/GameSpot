import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UsuarioResumen } from '../../../core/modelos';
import { botonCon, escribir } from '../../../pruebas/datos-admin';
import { AjustePuntos } from './ajuste-puntos';

const API = 'http://localhost:8080/api/admin/usuarios';

describe('AjustePuntos (CACS-18, criterio 5)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<AjustePuntos>;
  let elemento: HTMLElement;

  const laura: UsuarioResumen = {
    id: 5,
    nombre: 'Laura',
    email: 'laura.gamer@gamespot.test',
    rol: 'CLIENTE',
    puntosFidelidad: 3,
  };

  async function buscarYElegir() {
    escribir(elemento, '#busqueda', 'gamer');
    elemento.querySelector<HTMLFormElement>('form.busqueda')!.dispatchEvent(new Event('submit'));
    const busqueda = servidor.expectOne((r) => r.url === API && r.params.get('email') === 'gamer');
    busqueda.flush([laura]);
    await fixture.whenStable();
    botonCon(elemento, 'Laura').click();
    await fixture.whenStable();
  }

  function enviarAjuste(puntos: string, descripcion: string) {
    escribir(elemento, '#puntos', puntos);
    escribir(elemento, '#descripcion', descripcion);
    elemento
      .querySelector<HTMLFormElement>('form:not(.busqueda)')!
      .dispatchEvent(new Event('submit'));
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [AjustePuntos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AjustePuntos);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  it('CP-F12a busca al cliente por correo y registra un ajuste con el nuevo saldo', async () => {
    await buscarYElegir();
    expect(elemento.textContent).toContain('Laura tiene 3 puntos');

    enviarAjuste('5', ' Bono de bienvenida ');
    const ajuste = servidor.expectOne(`${API}/5/puntos`);
    expect(ajuste.request.body).toEqual({ puntos: 5, descripcion: 'Bono de bienvenida' });
    ajuste.flush({ usuarioId: 5, saldoPuntos: 8, movimientos: [] });
    await fixture.whenStable();

    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Nuevo saldo de Laura: 8 puntos',
    );
    expect(elemento.textContent).toContain('Laura tiene 8 puntos');
  });

  it('CP-F12b un saldo que quedaría negativo (409) muestra el mensaje del backend', async () => {
    await buscarYElegir();

    enviarAjuste('-10', 'Canje');
    servidor.expectOne(`${API}/5/puntos`).flush(
      {
        status: 409,
        mensaje: 'El ajuste de -10 puntos dejaría el saldo en negativo (saldo actual: 3)',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'dejaría el saldo en negativo',
    );
    expect(elemento.textContent).toContain('Laura tiene 3 puntos');
  });

  it('CP-F12c no busca con menos de 3 caracteres ni ajusta cero puntos o sin descripción', async () => {
    escribir(elemento, '#busqueda', 'ab');
    elemento.querySelector<HTMLFormElement>('form.busqueda')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    servidor.expectNone((r) => r.url === API);
    expect(elemento.textContent).toContain('Escribe al menos 3 caracteres.');

    await buscarYElegir();
    enviarAjuste('0', '');
    await fixture.whenStable();

    servidor.expectNone(`${API}/5/puntos`);
    expect(elemento.textContent).toContain('Escribe un número distinto de cero.');
    expect(elemento.textContent).toContain('Explica el motivo del ajuste.');
  });

  it('CP-F12d una búsqueda sin coincidencias lo dice', async () => {
    escribir(elemento, '#busqueda', 'nadie');
    elemento.querySelector<HTMLFormElement>('form.busqueda')!.dispatchEvent(new Event('submit'));
    servidor.expectOne((r) => r.url === API).flush([]);
    await fixture.whenStable();

    expect(elemento.textContent).toContain('No hay usuarios con ese correo.');
  });
});
