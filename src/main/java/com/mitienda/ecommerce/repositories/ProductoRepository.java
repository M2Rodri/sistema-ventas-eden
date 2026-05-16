package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con productos
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Buscar producto por SKU
     */
    Optional<Producto> findBySku(String sku);

    /**
     * Verificar si existe un producto con ese SKU
     */
    Boolean existsBySku(String sku);

    /**
     * Listar solo productos activos
     */
    List<Producto> findByActivoTrue();

    /**
     * Filtrar por categoría
     */
    List<Producto> findByCategoriaId(Long categoriaId);

    /**
     * Filtrar por tipo de producto
     */
    List<Producto> findByTipoProducto(TipoProducto tipoProducto);

    /**
     * Buscar productos por nombre (búsqueda parcial)
     */
    @Query("SELECT p FROM Producto p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Producto> searchByNombre(String nombre);

    /**
     * Contar productos activos
     */
    Long countByActivo(Boolean activo);

    /**
     * Contar productos por categoría
     */
    Long countByCategoriaId(Long categoriaId);
}