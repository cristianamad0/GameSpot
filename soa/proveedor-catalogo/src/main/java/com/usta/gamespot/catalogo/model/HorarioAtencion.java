package com.usta.gamespot.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Horario de un día de la semana (1 = lunes ... 7 = domingo). Tabla: horarios_atencion. */
@Entity
@Table(name = "horarios_atencion")
@Getter
@Setter
@NoArgsConstructor
public class HorarioAtencion {

  @Id
  @Column(name = "dia_semana")
  private Short diaSemana;

  @Column(name = "hora_apertura", nullable = false)
  private LocalTime horaApertura;

  @Column(name = "hora_cierre", nullable = false)
  private LocalTime horaCierre;
}
