package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a la tabla de usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    /** Busca por nombre de usuario (login). */
    Optional<Usuario> findByUsuario(String usuario);

    /** Si existe un usuario con ese nombre. */
    Boolean existsByUsuario(String usuario);

    /** Cuenta los usuarios según su estado activo. */
    Long countByActivo(Boolean activo);
}
