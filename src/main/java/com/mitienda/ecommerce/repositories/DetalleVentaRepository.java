package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para detalle de ventas
 */
@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /**
     * Obtener detalles de una venta
     */
    List<DetalleVenta> findByVentaId(Long ventaId);

    /**
     * Eliminar detalles de una venta
     */
    void deleteByVentaId(Long ventaId);
}