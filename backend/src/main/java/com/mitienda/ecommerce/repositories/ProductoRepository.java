package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con productos
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /*
     * Sobre los @EntityGraph de este repositorio
     *
     * Los DTO de respuesta recorren categoria e imagenes de cada producto. Con
     * la carga perezosa por defecto, Hibernate resolvia eso con una consulta
     * extra POR PRODUCTO: listar 8 productos disparaba 17 viajes a la base.
     *
     * Con la base en la misma maquina eso costaba milisegundos y no se notaba.
     * Con la base en Supabase, cada viaje cuesta unos 200 ms de ida y vuelta a
     * Oregon, y la pantalla de productos tardaba mas de 3 segundos.
     *
     * @EntityGraph le pide a Hibernate que traiga las relaciones en la misma
     * consulta. Pasa de 1 + 2N viajes a uno solo.
     */

    /** Listado completo, con categoria e imagenes ya cargadas. */
    @Override
    @EntityGraph(attributePaths = {"categoria", "imagenes"})
    List<Producto> findAll();

    /** Un producto con todo lo que necesita su ficha, en una sola consulta. */
    @Override
    @EntityGraph(attributePaths = {"categoria", "imagenes"})
    Optional<Producto> findById(Long id);

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
    @EntityGraph(attributePaths = {"categoria", "imagenes"})
    List<Producto> findByActivoTrue();

    /**
     * Filtrar por categoría
     */
    List<Producto> findByCategoriaId(Long categoriaId);

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