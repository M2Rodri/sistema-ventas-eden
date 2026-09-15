package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para detalle de ventas
 */
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /**
     * Obtener detalles de una venta
     */
    List<DetalleVenta> findByVentaId(Long ventaId);

    /**
     * Eliminar detalles de una venta
     */
    void deleteByVentaId(Long ventaId);

    /**
     * Productos mas vendidos dentro de un rango de fechas.
     *
     * Antes esto se resolvia trayendo toda la tabla detalle_venta a memoria con
     * findAll() y agrupando en Java. Tres problemas de eso, que esta consulta
     * corrige:
     *
     *   1. No filtraba por estado, asi que una venta CANCELADA seguia contando
     *      como producto vendido. El ranking mentia.
     *   2. No tenia rango de fechas, asi que solo podia responder "de toda la
     *      historia", nunca "de hoy".
     *   3. Con dos anios de ventas, traer la tabla entera a memoria en cada
     *      carga del panel se vuelve inviable. Ahora suma la base de datos y
     *      solo viajan las filas del ranking.
     *
     * Devuelve, por fila: id del producto, nombre, sku, unidades vendidas y
     * monto total. Se usa Object[] y no una proyeccion con constructor porque
     * el DTO de destino es una clase anidada dentro de DashboardResponse.
     *
     * El limite se pasa con Pageable (por ejemplo PageRequest.of(0, 10)),
     * que es como JPQL expresa el "LIMIT".
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
