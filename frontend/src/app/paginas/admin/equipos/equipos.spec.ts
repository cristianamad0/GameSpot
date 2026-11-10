import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EquipoAdmin, ModalidadAdmin } from '../../../core/modelos';
import { botonCon, escribir } from '../../../pruebas/datos-admin';
import { Equipos, textoReservasFuturas, textoSiguenActivas } from './equipos';

const API = 'http://localhost:8080/api/admin';

function equipo(id: number, nombre: string, cambios: Partial<EquipoAdmin> = {}): EquipoAdmin {
  return {
    id,
    nombre,
    modalidadId: 1,
    modalidad: 'Xbox 360',
    tarifaHoraPesos: 5000,
    modalidadEstaActiva: true,
    estaDisponible: true,
    reservasActivasFuturas: 0,
    ...cambios,
  };
}

const modalidades: ModalidadAdmin[] = [
  { id: 1, nombre: 'Xbox 360', tarifaHoraPesos: 5000, estaActiva: true, cantidadEquipos: 1 },
  { id: 2, nombre: 'Xbox Series S', tarifaHoraPesos: 6000, estaActiva: true, cantidadEquipos: 4 },
];

describe('Equipos (CACS-22, criterios 1 a 5)', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<Equipos>;
  let elemento: HTMLElement;

  const filaEquipo = (id: number) => elemento.querySelector<HTMLElement>(`[data-equipo="${id}"]`)!;
  const filaModalidad = (id: number) =>
    elemento.querySelector<HTMLElement>(`[data-modalidad="${id}"]`)!;

  async function crear(equipos: EquipoAdmin[]) {
    fixture = TestBed.createComponent(Equipos);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    servidor.expectOne(`${API}/equipos`).flush(equipos);
    servidor.expectOne(`${API}/modalidades`).flush(modalidades);
    await fixture.whenStable();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Equipos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('CP-F20a muestra cada equipo con modalidad, tarifa, estado en texto y reservas futuras', async () => {
    await crear([
      equipo(1, 'Xbox 360 #1', { reservasActivasFuturas: 2 }),
      equipo(2, 'Xbox Series S #1', {
        modalidad: 'Xbox Series S',
        tarifaHoraPesos: 6000,
        estaDisponible: false,
      }),
      equipo(3, 'Xbox Series S #2', { modalidadEstaActiva: false }),
    ]);

    const xbox = filaEquipo(1).textContent!.replace(/\s/g, ' ');
    expect(xbox).toContain('Xbox 360 #1');
    expect(xbox).toContain('$ 5.000 / hora');
    expect(xbox).toContain('Disponible');
    expect(xbox).toContain('2 reservas futuras');
    expect(filaEquipo(2).textContent).toContain('Fuera de servicio');
    expect(filaEquipo(2).textContent).toContain('Habilitar');
    expect(filaEquipo(3).textContent).toContain('Modalidad desactivada');
  });

  it('CP-F20b pone fuera de servicio un equipo sin reservas futuras sin pedir confirmación', async () => {
    await crear([equipo(1, 'Xbox 360 #1')]);

    botonCon(filaEquipo(1), 'Poner fuera de servicio').click();
    const peticion = servidor.expectOne(`${API}/equipos/1/disponibilidad`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ estaDisponible: false });
    peticion.flush(equipo(1, 'Xbox 360 #1', { estaDisponible: false }));
    await fixture.whenStable();

    expect(filaEquipo(1).textContent).toContain('Fuera de servicio');
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Xbox 360 #1 quedó fuera de servicio y ya no se puede reservar.',
    );
  });

  it('CP-F20c con reservas futuras pide confirmación y advierte que siguen activas', async () => {
    await crear([equipo(1, 'Xbox 360 #1', { reservasActivasFuturas: 2 })]);

    botonCon(filaEquipo(1), 'Poner fuera de servicio').click();
    await fixture.whenStable();
    servidor.expectNone(`${API}/equipos/1/disponibilidad`);
    const dialogo = elemento.querySelector('[role="alertdialog"]')!;
    expect(dialogo.textContent).toContain('2 reservas futuras');

    botonCon(elemento, 'Cancelar').click();
    await fixture.whenStable();
    expect(elemento.querySelector('[role="alertdialog"]')).toBeNull();

    botonCon(filaEquipo(1), 'Poner fuera de servicio').click();
    await fixture.whenStable();
    botonCon(elemento, 'Sí, fuera de servicio').click();
    servidor
      .expectOne(`${API}/equipos/1/disponibilidad`)
      .flush(equipo(1, 'Xbox 360 #1', { estaDisponible: false, reservasActivasFuturas: 2 }));
    await fixture.whenStable();

    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Sus 2 reservas futuras siguen activas: avisa a los clientes.',
    );
  });

  it('CP-F20d habilita un equipo fuera de servicio', async () => {
    await crear([equipo(1, 'Xbox 360 #1', { estaDisponible: false })]);

    botonCon(filaEquipo(1), 'Habilitar').click();
    const peticion = servidor.expectOne(`${API}/equipos/1/disponibilidad`);
    expect(peticion.request.body).toEqual({ estaDisponible: true });
    peticion.flush(equipo(1, 'Xbox 360 #1'));
    await fixture.whenStable();

    expect(filaEquipo(1).textContent).toContain('Disponible');
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'quedó disponible para reservar',
    );
  });

  it('CP-F20e cambia la tarifa de una modalidad y avisa que las reservas existentes no cambian', async () => {
    await crear([equipo(1, 'Xbox 360 #1')]);

    escribir(filaModalidad(1), '#tarifa-1', '8000');
    await fixture.whenStable();
    botonCon(filaModalidad(1), 'Guardar').click();
    const peticion = servidor.expectOne(`${API}/modalidades/1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ tarifaHoraPesos: 8000, estaActiva: true });
    peticion.flush({ ...modalidades[0], tarifaHoraPesos: 8000 });
    await fixture.whenStable();

    // Se vuelve a cargar la lista de equipos para mostrar la tarifa nueva.
    servidor
      .expectOne(`${API}/equipos`)
      .flush([equipo(1, 'Xbox 360 #1', { tarifaHoraPesos: 8000 })]);
    await fixture.whenStable();

    const aviso = elemento.querySelector('[role="status"]')!.textContent!.replace(/\s/g, ' ');
    expect(aviso).toContain('Xbox 360: $ 8.000 por hora');
    expect(aviso).toContain('Las reservas que ya existen conservan su precio.');
    expect(filaEquipo(1).textContent!.replace(/\s/g, ' ')).toContain('$ 8.000 / hora');
  });

  it('CP-F20f desactiva una modalidad desde su casilla', async () => {
    await crear([equipo(1, 'Xbox 360 #1')]);

    const casilla = filaModalidad(2).querySelector<HTMLInputElement>('input[type="checkbox"]')!;
    casilla.checked = false;
    casilla.dispatchEvent(new Event('change'));
    botonCon(filaModalidad(2), 'Guardar').click();
    const peticion = servidor.expectOne(`${API}/modalidades/2`);
    expect(peticion.request.body).toEqual({ tarifaHoraPesos: 6000, estaActiva: false });
    peticion.flush({ ...modalidades[1], estaActiva: false });
    await fixture.whenStable();
    servidor.expectOne(`${API}/equipos`).flush([equipo(1, 'Xbox 360 #1')]);
    await fixture.whenStable();

    expect(elemento.querySelector('[role="status"]')?.textContent).toContain('desactivada');
  });

  it('CP-F20g una tarifa no positiva o sobre el tope no se envía y un 404 se muestra con el mensaje del backend', async () => {
    await crear([equipo(1, 'Xbox 360 #1')]);

    escribir(filaModalidad(1), '#tarifa-1', '0');
    await fixture.whenStable();
    botonCon(filaModalidad(1), 'Guardar').click();
    await fixture.whenStable();
    servidor.expectNone(`${API}/modalidades/1`);
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'La tarifa de Xbox 360 debe ser un número entero entre 1 y',
    );

    // Tampoco se envía una tarifa por encima del tope de $1.000.000.
    escribir(filaModalidad(1), '#tarifa-1', '1000001');
    await fixture.whenStable();
    botonCon(filaModalidad(1), 'Guardar').click();
    await fixture.whenStable();
    servidor.expectNone(`${API}/modalidades/1`);
    expect(elemento.querySelector('[role="alert"]')?.textContent?.replace(/\s/g, ' ')).toContain(
      'entre 1 y $ 1.000.000.',
    );

    botonCon(filaEquipo(1), 'Poner fuera de servicio').click();
    servidor
      .expectOne(`${API}/equipos/1/disponibilidad`)
      .flush(
        { status: 404, mensaje: 'No existe el equipo con id 1' },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();
    expect(elemento.textContent).toContain('No existe el equipo con id 1');
  });

  it('CP-F20h los textos de reservas futuras concuerdan en singular y plural', () => {
    expect(textoReservasFuturas(1)).toBe('1 reserva futura');
    expect(textoReservasFuturas(3)).toBe('3 reservas futuras');
    expect(textoSiguenActivas(1)).toBe('Su reserva futura sigue activa');
    expect(textoSiguenActivas(2)).toBe('Sus 2 reservas futuras siguen activas');
  });
});
