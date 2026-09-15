package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.MultimediaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MultimediaProductoRepository extends JpaRepository<MultimediaProducto, Long> {

    /**
     * Obtener multimedia por producto
     */
    Optional<MultimediaProducto> findByProductoId(Long productoId);

    /**
     * Productos con RA habilitado
     */
    List<MultimediaProducto> findByHabilitadoRaTrueAndActivoTrue();

    /**
     * Multimedia activa
     */
    List<MultimediaProducto> findByActivoTrue();
}