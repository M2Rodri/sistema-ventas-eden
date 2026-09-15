// src/main/java/com/mitienda/ecommerce/repositories/InventarioRepository.java
package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Inventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de inventario
 */
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    /*
     * InventarioResponse lee producto, y ademas la categoria del producto. Sin
     * esto eran dos consultas extra por cada fila del inventario; la pantalla
     * tardaba mas de 3 segundos contra Supabase. Ver el comentario equivalente
     * en ProductoRepository.
     */

    /** Listado completo, con el producto y su categoria ya cargados. */
    @Override
    @EntityGraph(attributePaths = {"producto", "producto.categoria"})
    List<Inventario> findAll();

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