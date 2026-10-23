package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Inventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositorio para gestión de inventario */
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    // Trae producto y categoría en la misma consulta para evitar dos consultas extra por fila.

    /** Listado completo, con el producto y su categoria ya cargados. */
    @Override
    @EntityGraph(attributePaths = {"producto", "producto.categoria"})
    List<Inventario> findAll();

    /** Buscar inventario por ID de producto */
    Optional<Inventario> findByProductoId(Long idProducto);

    /** Verificar si existe inventario para un producto */
    boolean existsByProductoId(Long idProducto);

    /** Obtener productos con stock bajo (stock <= stock mínimo; un mínimo de 0 no genera alerta) */
    @Query("SELECT i FROM Inventario i WHERE i.producto.stockMinimo > 0 AND i.cantidadDisponible <= i.producto.stockMinimo")
    List<Inventario> findProductosConStockBajo();

    /** Obtener productos sin stock (stock = 0) */
    @Query("SELECT i FROM Inventario i WHERE i.cantidadDisponible = 0")
    List<Inventario> findProductosSinStock();

    /** Contar productos con stock bajo */
    @Query("SELECT COUNT(i) FROM Inventario i WHERE i.producto.stockMinimo > 0 AND i.cantidadDisponible <= i.producto.stockMinimo")
    Long countProductosConStockBajo();

    /**
     * Catálogo con precio y stock en una consulta (ver InventarioController#getCatalogoApp). Parte de Inventario
     * porque todo producto activo tiene su fila. CAST(:nombre AS string) evita el error lower(bytea) de Postgres con parámetros null.
     */
    @EntityGraph(attributePaths = {"producto", "producto.categoria", "producto.imagenes"})
    @Query("SELECT i FROM Inventario i "
            + "WHERE i.producto.activo = true "
            + "AND (:nombre IS NULL OR LOWER(i.producto.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%'))) "
            + "AND (:soloBajoMinimo = false OR (i.producto.stockMinimo > 0 AND i.cantidadDisponible <= i.producto.stockMinimo)) "
            + "ORDER BY i.producto.nombre")
    List<Inventario> findCatalogoApp(@Param("nombre") String nombre, @Param("soloBajoMinimo") boolean soloBajoMinimo);

    /** Cifras del panel de Inicio en una consulta: [sinStock, bajoStock, valorAPrecioDeCompra]. */
    @Query("SELECT COUNT(CASE WHEN i.cantidadDisponible = 0 THEN 1 END), "
            + "COUNT(CASE WHEN p.stockMinimo > 0 AND i.cantidadDisponible <= p.stockMinimo THEN 1 END), "
            + "COALESCE(SUM(CASE WHEN p.precioCompra IS NOT NULL THEN p.precioCompra * i.cantidadDisponible END), 0) "
            + "FROM Inventario i JOIN i.producto p")
    List<Object[]> resumenInventario();
}
