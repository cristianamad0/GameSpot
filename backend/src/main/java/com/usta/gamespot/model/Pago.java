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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Pago del adelanto de una reserva con Wompi. Tabla: pagos.
 *
 * <p>Al guardar un pago en APROBADO, el trigger pagos_confirmar_reserva confirma la reserva en
 * la base de datos. Hibernate no se entera de ese cambio: si se necesita la reserva actualizada
 * en la misma transacción, hay que volver a leerla con entityManager.refresh(reserva).
 *
 * <p>Nunca guardar aquí llaves de Wompi ni datos de tarjeta.
 */
@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reserva_id", nullable = false)
  private Reserva reserva;

  /** Referencia única que el sistema envía a Wompi. */
  @Column(nullable = false, unique = true)
  private String referencia;

  /** Id de la transacción que devuelve Wompi. */
  @Column(name = "id_transaccion_wompi", unique = true)
  private String idTransaccionWompi;

  /** Wompi maneja centavos: $2.000 COP = 200000 centavos. */
  @Column(name = "monto_centavos", nullable = false)
  private Long montoCentavos;

  @Column(nullable = false)
  @Builder.Default
  private String moneda = "COP";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private EstadoPago estado = EstadoPago.PENDIENTE;

  /** CARD, NEQUI, PSE, BANCOLOMBIA_TRANSFER... */
  @Column(name = "metodo_pago")
  private String metodoPago;

  /** Último evento recibido del webhook de Wompi, como JSON. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "respuesta_wompi", columnDefinition = "jsonb")
  private String respuestaWompi;

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
