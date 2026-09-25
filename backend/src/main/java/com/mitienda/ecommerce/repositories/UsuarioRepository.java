package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para operaciones de base de datos con la entidad Usuario
 * Spring Data JPA genera automáticamente las implementaciones
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Buscar usuario por nombre de usuario (login)
     * @param usuario Nombre de usuario
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByUsuario(String usuario);

    /**
     * Verificar si existe un usuario con ese nombre de usuario
     * @param usuario Nombre de usuario a verificar
     * @return true si existe, false si no
     */
    Boolean existsByUsuario(String usuario);

    /**
     * Contar usuarios activos
     * @param activo Estado del usuario
     * @return Cantidad de usuarios activos
     */
    Long countByActivo(Boolean activo);
}
