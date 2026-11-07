package com.usta.gamespot.repository;

import com.usta.gamespot.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    /** CACS-18: hasta 10 usuarios cuyo correo contiene el texto, sin distinguir mayúsculas. */
    List<Usuario> findTop10ByEmailContainingIgnoreCaseOrderByEmailAsc(String texto);
}
