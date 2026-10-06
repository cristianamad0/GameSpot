package com.usta.gamespot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Horario de atención de un día de la semana. Tabla: horarios_atencion. */
@Entity
@Table(name = "horarios_atencion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioAtencion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Igual a DayOfWeek.getValue(): 1 = lunes, 7 = domingo. */
  @Column(name = "dia_semana", nullable = false, unique = true)
  private Short diaSemana;

  @Column(name = "hora_apertura", nullable = false)
  private LocalTime horaApertura;

  @Column(name = "hora_cierre", nullable = false)
  private LocalTime horaCierre;
}
