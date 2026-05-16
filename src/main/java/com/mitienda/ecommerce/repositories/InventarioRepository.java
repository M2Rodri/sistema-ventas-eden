// src/main/java/com/mitienda/ecommerce/repositories/InventarioRepository.java
package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de inventario
 */
@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    /**
     * Buscar inventario por ID de producto
     */
    Optional<Inventario> findByProductoId(Long idProducto);

    /**
     * Verificar si existe inventario para un producto
     */
    boolean existsByProductoId(Long idProducto);

    /**
     * Obtener productos con stock bajo (stock <= stock mínimo)
     */
    @Query("SELECT i FROM Inventario i WHERE i.cantidadDisponible <= i.producto.stockMinimo")
    List<Inventario> findProductosConStockBajo();

    /**
     * Obtener productos sin stock (stock = 0)
     */
    @Query("SELECT i FROM Inventario i WHERE i.cantidadDisponible = 0")
    List<Inventario> findProductosSinStock();

    /**
     * Contar productos con stock bajo
     */
    @Query("SELECT COUNT(i) FROM Inventario i WHERE i.cantidadDisponible <= i.producto.stockMinimo")
    Long countProductosConStockBajo();
}