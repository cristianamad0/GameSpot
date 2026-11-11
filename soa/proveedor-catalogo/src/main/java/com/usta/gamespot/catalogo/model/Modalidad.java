package com.usta.gamespot.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tipo de equipo con su tarifa por hora, por ejemplo "Xbox Series S". Tabla: modalidades. */
@Entity
@Table(name = "modalidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modalidad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String nombre;

  @Column(name = "tarifa_hora_pesos", nullable = false)
  private Integer tarifaHoraPesos;

  @Column(name = "esta_activa", nullable = false)
  @Builder.Default
  private boolean estaActiva = true;
}
