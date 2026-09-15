package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con categorías
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * Buscar categoría por nombre
     */
    Optional<Categoria> findByNombre(String nombre);

    /**
     * Verificar si existe una categoría con ese nombre
     */
    Boolean existsByNombre(String nombre);

    /**
     * Listar solo categorías activas
     */
    List<Categoria> findByActivoTrue();

    /**
     * Contar categorías activas
     */
    Long countByActivo(Boolean activo);
}