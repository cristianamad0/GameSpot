package com.usta.gamespot.reservas.model;

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
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reserva de un equipo en bloques de 30 minutos. Tabla: reservas.
 *
 * <p>El equipo vive en el proveedor de catálogo: aquí solo se guarda su id y una copia de su
 * nombre y del precio del momento, así la reserva no cambia si después se edita el catálogo.
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

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "equipo_id", nullable = false)
  private Long equipoId;

  @Column(name = "nombre_equipo", nullable = false)
  private String nombreEquipo;

  @Column(nullable = false)
  private Instant inicio;

  @Column(nullable = false)
  private Instant fin;

  @Column(name = "precio_total_pesos", nullable = false)
  private Integer precioTotalPesos;

  @Column(name = "adelanto_pesos", nullable = false)
  private Integer adelantoPesos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private EstadoReserva estado = EstadoReserva.PENDIENTE_PAGO;

  @Column(name = "creada_en", nullable = false)
  private Instant creadaEn;

  @Column(name = "cancelada_en")
  private Instant canceladaEn;
}
