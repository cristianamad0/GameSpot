package com.usta.gamespot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Consola o sala física que se reserva. Tabla: equipos. */
@Entity
@Table(name = "equipos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modalidad_id", nullable = false)
  private Modalidad modalidad;

  /** Por ejemplo, "Xbox 360 #1". */
  @Column(nullable = false, unique = true)
  private String nombre;

  /** false = fuera de servicio; no se ofrece para reservar. */
  @Column(name = "esta_disponible", nullable = false)
  @Builder.Default
  private boolean estaDisponible = true;

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
