package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para operaciones de base de datos con la entidad User
 * Spring Data JPA genera automáticamente las implementaciones
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Buscar usuario por email
     * @param email Email del usuario
     * @return Optional con el usuario si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Verificar si existe un usuario con ese email
     * @param email Email a verificar
     * @return true si existe, false si no
     */
    Boolean existsByEmail(String email);

    /**
     * Contar usuarios activos
     * @param activo Estado del usuario
     * @return Cantidad de usuarios activos
     */
    Long countByActivo(Boolean activo);
}
