package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con imágenes de productos
 */
@Repository
public interface ImagenProductoRepository extends JpaRepository<ImagenProducto, Long> {

    /**
     * Obtener todas las imágenes de un producto
     */
    List<ImagenProducto> findByProductoIdOrderByOrdenAsc(Long productoId);

    /**
     * Obtener imagen principal de un producto
     */
    Optional<ImagenProducto> findByProductoIdAndEsPrincipalTrue(Long productoId);

    /**
     * Eliminar todas las imágenes de un producto
     */
    void deleteByProductoId(Long productoId);
}