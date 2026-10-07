package com.usta.gamespot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tipo de servicio con su tarifa (Xbox 360, Series S, Sala VIP). Tabla: modalidades. */
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

  private String descripcion;

  @Column(name = "tarifa_hora_pesos", nullable = false)
  private Integer tarifaHoraPesos;

  @Column(name = "esta_activa", nullable = false)
  @Builder.Default
  private boolean estaActiva = true;

  @Column(name = "creado_en", nullable = false, updatable = false, columnDefinition = "timestamptz")
  private Instant creadoEn;

  @Column(name = "actualizado_en", nullable = false, columnDefinition = "timestamptz")
  private Instant actualizadoEn;

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
    actualizadoEn = creadoEn;
  }

  @PreUpdate
  void alActualizar() {
    actualizadoEn = Instant.now();
  }
}
