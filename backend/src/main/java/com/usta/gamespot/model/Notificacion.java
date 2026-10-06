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

/** Confirmación o recordatorio de una reserva. Tabla: notificaciones. */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reserva_id")
  private Reserva reserva;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoNotificacion tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private CanalNotificacion canal = CanalNotificacion.CORREO;

  @Column(name = "programada_para", nullable = false, columnDefinition = "timestamptz")
  private Instant programadaPara;

  @Column(name = "fue_enviada", nullable = false)
  @Builder.Default
  private boolean fueEnviada = false;

  /** Obligatorio cuando fueEnviada = true. */
  @Column(name = "enviada_en", columnDefinition = "timestamptz")
  private Instant enviadaEn;

  @Column(nullable = false)
  @Builder.Default
  private Integer intentos = 0;

  @Column(name = "ultimo_error")
  private String ultimoError;

  @Column(name = "creado_en", nullable = false, updatable = false, columnDefinition = "timestamptz")
  private Instant creadoEn;

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
  }
}
