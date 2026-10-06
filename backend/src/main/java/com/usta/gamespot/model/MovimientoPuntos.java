package com.usta.gamespot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Suma o resta de puntos de fidelización. Tabla: movimientos_puntos.
 *
 * <p>Los puntos por horas jugadas los crea la base de datos cuando una reserva pasa a
 * COMPLETADA; el backend solo crea movimientos AJUSTE_ADMIN. Cada movimiento actualiza
 * automáticamente usuarios.puntos_fidelidad.
 */
@Entity
@Table(name = "movimientos_puntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoPuntos {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reserva_id")
  private Reserva reserva;

  /** Positivo suma, negativo resta. Nunca cero. */
  @Column(nullable = false)
  private Integer puntos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoMovimientoPuntos tipo;

  private String descripcion;

  @Column(name = "creado_en", nullable = false, updatable = false, columnDefinition = "timestamptz")
  private Instant creadoEn;

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
  }
}
