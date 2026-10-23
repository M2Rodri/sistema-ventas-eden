package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Repositorio para detalle de ventas */
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    /** Obtener detalles de una venta */
    List<DetalleVenta> findByVentaId(Long ventaId);

    /** Eliminar detalles de una venta */
    void deleteByVentaId(Long ventaId);

    /**
     * Productos más vendidos en un rango, solo ventas COMPLETADA, agrupados en la base.
     * Cada fila: id del producto, nombre, sku, unidades y monto. El límite se pasa con Pageable.
     */
    @Query("SELECT d.producto.id, d.producto.nombre, d.producto.sku, "
            + "SUM(d.cantidad), SUM(d.subtotal) "
            + "FROM DetalleVenta d "
            + "WHERE d.venta.estado = com.mitienda.ecommerce.models.EstadoVenta.COMPLETADA "
            + "AND d.venta.fechaVenta >= :desde AND d.venta.fechaVenta < :hasta "
            + "GROUP BY d.producto.id, d.producto.nombre, d.producto.sku "
            + "ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> findMasVendidosEntre(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta,
                                        Pageable limite);
}
