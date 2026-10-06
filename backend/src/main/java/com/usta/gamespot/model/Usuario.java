package com.usta.gamespot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa a un usuario de Game Spot (cliente o administrador).
 * Cubre la tarjeta CACS-2 (registro/login) y sirve de base para
 * CACS-3 (historial de reservas), que colgara de este mismo usuario.
 */
@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Rol rol = Rol.CLIENTE;

    /**
     * Saldo de puntos. Lo mantiene la base de datos con cada movimiento de puntos;
     * Hibernate solo lo lee para no sobrescribirlo con un valor desactualizado.
     */
    @Column(name = "puntos_fidelidad", nullable = false, insertable = false, updatable = false)
    @Builder.Default
    private Integer puntosFidelidad = 0;

    @Column(name = "fecha_registro", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private LocalDateTime fechaRegistro;

    @PrePersist
    void alCrear() {
        this.fechaRegistro = LocalDateTime.now();
    }
}
