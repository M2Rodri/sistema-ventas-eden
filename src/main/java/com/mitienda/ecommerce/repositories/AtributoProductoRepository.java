package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.AtributoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para atributos de productos
 */
@Repository
public interface AtributoProductoRepository extends JpaRepository<AtributoProducto, Long> {

    /**
     * Obtener todos los atributos de un producto
     */
    List<AtributoProducto> findByProductoId(Long productoId);

    /**
     * Eliminar todos los atributos de un producto
     */
    void deleteByProductoId(Long productoId);

    /**
     * Buscar atributo específico de un producto
     */
    List<AtributoProducto> findByProductoIdAndNombreAtributo(Long productoId, String nombreAtributo);
}