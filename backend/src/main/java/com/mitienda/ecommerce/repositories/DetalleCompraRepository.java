package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para detalle de compras
 */
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {

    /**
     * Obtener detalles de una compra
     */
    List<DetalleCompra> findByCompraId(Long compraId);

    /**
     * Eliminar detalles de una compra
     */
    void deleteByCompraId(Long compraId);
}