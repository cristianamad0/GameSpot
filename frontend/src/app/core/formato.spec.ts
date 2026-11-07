import { hoyEnNegocio, horaNegocio, pesos, textoPuntos, tiempoRestante } from './formato';

describe('Formatos del negocio (CACS-18)', () => {
  it('CP-F14a muestra las horas en la zona de Bogotá (UTC-5)', () => {
    // 20:00 UTC = 3:00 p. m. en Bogotá.
    expect(horaNegocio('2026-11-10T20:00:00Z')).toMatch(/^3:00\s?p\.\s?m\.$/);
  });

  it('CP-F14b la fecha de hoy es la de Bogotá, aunque en UTC ya sea el día siguiente', () => {
    // 2:00 a. m. UTC del 11 = 9:00 p. m. del 10 en Bogotá.
    expect(hoyEnNegocio(new Date('2026-11-11T02:00:00Z'))).toBe('2026-11-10');
  });

  it('CP-F14c los valores se muestran en pesos sin decimales', () => {
    expect(pesos(16000).replace(/\s/g, ' ')).toBe('$ 16.000');
  });

  it('CP-F14d los puntos usan singular y plural', () => {
    expect(textoPuntos(1)).toBe('1 punto');
    expect(textoPuntos(-1)).toBe('-1 punto');
    expect(textoPuntos(8)).toBe('8 puntos');
    expect(textoPuntos(0)).toBe('0 puntos');
  });
});

describe('Cuenta regresiva del pago (CACS-17)', () => {
  it('CP-F14e el tiempo restante se muestra en horas, minutos o segundos, nunca negativo', () => {
    expect(tiempoRestante(65 * 60_000)).toBe('1 h 05 min');
    expect(tiempoRestante(14 * 60_000 + 9_000)).toBe('14 min 09 s');
    expect(tiempoRestante(45_000)).toBe('45 s');
    expect(tiempoRestante(-5_000)).toBe('0 s');
  });
});
