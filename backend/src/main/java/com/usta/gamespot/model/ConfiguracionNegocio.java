package com.usta.gamespot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Parámetros del negocio. Siempre existe una sola fila, con id = 1. */
@Entity
@Table(name = "configuracion_negocio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionNegocio {

  public static final short ID_UNICO = 1;

  @Id private Short id;

  @Column(name = "valor_adelanto_pesos", nullable = false)
  private Integer valorAdelantoPesos;

  @Column(name = "puntos_por_hora", nullable = false)
  private Integer puntosPorHora;

  /** Minutos que se aparta el bloque mientras el cliente paga en Wompi. */
  @Column(name = "minutos_espera_pago", nullable = false)
  private Integer minutosEsperaPago;

  @Column(name = "actualizado_en", nullable = false, columnDefinition = "timestamptz")
  private Instant actualizadoEn;

  @PrePersist
  @PreUpdate
  void alGuardar() {
    actualizadoEn = Instant.now();
  }
}
