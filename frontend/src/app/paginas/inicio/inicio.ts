import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Sesion } from '../../core/auth/sesion';

interface Modalidad {
  nombre: string;
  descripcion: string;
  icono: string;
  cantidad: string;
}

/** CACS-15: página de inicio de Game Spot. */
@Component({
  selector: 'gs-inicio',
  imports: [RouterLink],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  protected readonly sesion = inject(Sesion);

  /** Equipos reales del negocio (migración V2_datos_negocio.sql). */
  protected readonly modalidades: Modalidad[] = [
    {
      nombre: 'Xbox Series S',
      descripcion: 'Los títulos de la generación actual, con mando y pantalla para ti.',
      icono: '🎮',
      cantidad: '4 consolas',
    },
    {
      nombre: 'Xbox 360',
      descripcion: 'Los clásicos de siempre para jugar con amigos.',
      icono: '🕹️',
      cantidad: '1 consola',
    },
    {
      nombre: 'Sala VIP Xbox Series X',
      descripcion: 'Espacio privado para tu grupo, con la consola más potente.',
      icono: '👑',
      cantidad: '1 sala',
    },
  ];

  /** Horario de atención (horarios_atencion). */
  protected readonly horarios = [
    { dias: 'Lunes a viernes', horas: '2:00 p. m. – 10:00 p. m.' },
    { dias: 'Sábado y domingo', horas: '3:00 p. m. – 9:00 p. m.' },
  ];

  protected readonly pasos = [
    {
      titulo: 'Elige tu equipo',
      texto: 'Consola o sala, el día y la hora en bloques de 30 minutos.',
    },
    {
      titulo: 'Paga el adelanto',
      texto: 'Asegura tu turno pagando el 50 % antes de que venza el plazo.',
    },
    {
      titulo: 'Llega y juega',
      texto: 'Tu bloque queda reservado solo para ti y sumas puntos por jugar.',
    },
  ];
}
