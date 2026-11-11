package com.usta.gamespot.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario que puede iniciar sesión (CLIENTE o ADMIN). Tabla: usuarios. */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nombre;

  @Column(nullable = false, unique = true)
  private String email;

  /** Hash BCrypt de la contraseña; nunca la contraseña en texto plano. */
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  /** CLIENTE o ADMIN. */
  @Column(nullable = false)
  private String rol;
}
