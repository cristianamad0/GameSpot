package com.usta.gamespot.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Equipo físico que se puede reservar, por ejemplo "Xbox Series S #2". Tabla: equipos. */
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

  @Column(nullable = false, unique = true)
  private String nombre;

  /** Muchos equipos pertenecen a una modalidad (clave foránea modalidad_id). */
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "modalidad_id", nullable = false)
  private Modalidad modalidad;

  /** false cuando el equipo está fuera de servicio y no se puede reservar. */
  @Column(name = "esta_disponible", nullable = false)
  @Builder.Default
  private boolean estaDisponible = true;
}
