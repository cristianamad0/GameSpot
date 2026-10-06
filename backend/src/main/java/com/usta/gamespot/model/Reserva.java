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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reserva de uno o varios bloques seguidos de 30 minutos sobre un equipo. Tabla: reservas.
 *
 * <p>La base de datos lanza DataIntegrityViolationException si la reserva se cruza con otra
 * activa del mismo equipo (restricción reservas_sin_cruces) o si se intenta confirmar sin
 * adelanto pagado (reservas_confirmada_requiere_adelanto).
 */
@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "equipo_id", nullable = false)
  private Equipo equipo;

  /** Debe caer en :00 o :30. */
  @Column(nullable = false, columnDefinition = "timestamptz")
  private Instant inicio;

  /** La duración (fin - inicio) debe ser múltiplo de 30 minutos. */
  @Column(nullable = false, columnDefinition = "timestamptz")
  private Instant fin;

  /** Valor calculado con la tarifa vigente al momento de reservar. */
  @Column(name = "precio_total_pesos", nullable = false)
  private Integer precioTotalPesos;

  @Column(name = "adelanto_pesos", nullable = false)
  private Integer adelantoPesos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private EstadoReserva estado = EstadoReserva.PENDIENTE_PAGO;

  @Column(name = "tiene_anticipo_pagado", nullable = false)
  @Builder.Default
  private boolean tieneAnticipoPagado = false;

  /** Obligatorio mientras la reserva esté en PENDIENTE_PAGO. */
  @Column(name = "expira_en", columnDefinition = "timestamptz")
  private Instant expiraEn;

  /** Obligatorio cuando la reserva pasa a CANCELADA. */
  @Column(name = "cancelada_en", columnDefinition = "timestamptz")
  private Instant canceladaEn;

  @Column(name = "motivo_cancelacion")
  private String motivoCancelacion;

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
