import { TestBed } from '@angular/core/testing';
import { EstadoReserva } from '../../core/modelos';
import { EstadoReservaEtiqueta } from './estado-reserva';

describe('EstadoReservaEtiqueta (CACS-15, criterio 5)', () => {
  const casos: [EstadoReserva, string][] = [
    ['PENDIENTE_PAGO', 'Pendiente de pago'],
    ['CONFIRMADA', 'Confirmada'],
    ['COMPLETADA', 'Completada'],
    ['CANCELADA', 'Cancelada'],
    ['EXPIRADA', 'Expirada'],
  ];

  it.each(casos)(
    'CP-F07 el estado %s se muestra con texto ("%s"), no solo con color',
    async (estado, texto) => {
      const fixture = TestBed.createComponent(EstadoReservaEtiqueta);
      fixture.componentRef.setInput('estado', estado);
      await fixture.whenStable();

      const etiqueta = (fixture.nativeElement as HTMLElement).querySelector('.estado')!;
      expect(etiqueta.textContent?.trim()).toBe(texto);
      expect(etiqueta.getAttribute('data-estado')).toBe(estado);
    },
  );
});
