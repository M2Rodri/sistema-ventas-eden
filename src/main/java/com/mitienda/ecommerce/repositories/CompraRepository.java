package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Compra;
import com.mitienda.ecommerce.models.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para operaciones con compras
 */
@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    /**
     * Listar compras por proveedor
     */
    List<Compra> findByProveedorIdOrderByFechaCompraDesc(Long proveedorId);

    /**
     * Filtrar compras por estado
     */
    List<Compra> findByEstadoOrderByFechaCompraDesc(EstadoCompra estado);

    /**
     * Compras entre fechas
     */
    List<Compra> findByFechaCompraBetweenOrderByFechaCompraDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Contar compras por estado
     */
    Long countByEstado(EstadoCompra estado);

    /**
     * Últimas compras (límite)
     */
    List<Compra> findTop10ByOrderByFechaCompraDesc();

    /**
     * Total de compras en un rango de fechas
     */
    @Query("SELECT SUM(c.costoTotal) FROM Compra c WHERE c.fechaCompra BETWEEN :inicio AND :fin AND c.estado = 'RECIBIDA'")
    BigDecimal sumCostoTotalByFechaCompraBetween(LocalDateTime inicio, LocalDateTime fin);
}