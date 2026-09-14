package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de roles del personal.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Busca un rol por su nombre técnico (ADMIN, EMPLEADO). */
    Optional<Role> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
