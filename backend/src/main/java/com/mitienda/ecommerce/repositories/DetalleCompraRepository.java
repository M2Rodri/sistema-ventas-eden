package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.DetalleCompra;
import com.mitienda.ecommerce.models.EstadoCompra;
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

    /**
     * Si un producto ya tuvo alguna compra confirmada, su precio de compra
     * pasa a ser propiedad de Compras (ver ProductoService). Antes de eso,
     * el valor cargado en Productos es solo una estimación inicial y se
     * puede seguir corrigiendo desde ahí.
     */
    boolean existsByProducto_IdAndCompra_Estado(Long idProducto, EstadoCompra estado);
}