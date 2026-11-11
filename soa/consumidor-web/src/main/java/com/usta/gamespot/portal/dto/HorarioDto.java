package com.usta.gamespot.portal.dto;

import java.time.LocalTime;

/**
 * Horario de un día, del proveedor de catálogo (GET /api/horarios).
 *
 * @param diaSemana 1 = lunes ... 7 = domingo
 * @param horaApertura apertura
 * @param horaCierre cierre
 */
public record HorarioDto(short diaSemana, LocalTime horaApertura, LocalTime horaCierre) {}
