// src/main/java/com/mitienda/ecommerce/repositories/InventarioRepository.java
package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Inventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Catálogo con precio y stock en una sola consulta, para la app móvil
     * (Catálogo, Alertas de stock y Nueva venta comparten este mismo dato:
     * ver InventarioController#getCatalogoApp).
     *
     * Arranca desde Inventario, no desde Producto: cada producto activo ya
     * tiene su fila de inventario (se crea junto con el producto, ver
     * ProductoService.createProducto), así que no hace falta un LEFT JOIN.
     *
     * "nombre IS NULL OR ..." y "soloBajoMinimo = false OR ..." son el mismo
     * truco: cuando el filtro no aplica, esa mitad del OR es true y no
     * descarta filas.
     *
     * CAST(:nombre AS string): sin esto, Postgres tira "no existe la función
     * lower(bytea)" cada vez que nombre viaja null (o sea, siempre que no se
     * manda filtro). Al preparar la consulta, Postgres necesita el tipo de
     * cada parámetro de antemano; sin una pista explícita, un parámetro nulo
     * usado dentro de LOWER() lo resuelve mal. El cast fuerza el tipo y
     * saca la ambigüedad.
     */
    @EntityGraph(attributePaths = {"producto", "producto.categoria", "producto.imagenes"})
    @Query("SELECT i FROM Inventario i "
            + "WHERE i.producto.activo = true "
            + "AND (:nombre IS NULL OR LOWER(i.producto.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%'))) "
            + "AND (:soloBajoMinimo = false OR i.cantidadDisponible <= i.producto.stockMinimo) "
            + "ORDER BY i.producto.nombre")
    List<Inventario> findCatalogoApp(@Param("nombre") String nombre, @Param("soloBajoMinimo") boolean soloBajoMinimo);
}