package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con ventas
 * Soporta búsquedas tanto por cliente registrado como cliente directo
 */
@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Listar ventas por cliente registrado
     */
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);

    /**
     * Buscar ventas por nombre de cliente (tanto registrado como directo)
     * Busca en nombre y apellido del cliente registrado, o en nombreClienteDirecto
     */
    @Query("SELECT v FROM Venta v WHERE " +
           "LOWER(v.cliente.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
           "LOWER(v.cliente.apellido) LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
           "LOWER(v.nombreClienteDirecto) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByNombreClienteContaining(@Param("nombre") String nombre);

    /**
     * Buscar ventas por celular de cliente (tanto registrado como directo)
     */
    @Query("SELECT v FROM Venta v WHERE " +
           "v.cliente.celular LIKE CONCAT('%', :celular, '%') OR " +
           "v.celularClienteDirecto LIKE CONCAT('%', :celular, '%') " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByCelularClienteContaining(@Param("celular") String celular);

    /**
     * Filtrar ventas por estado
     */
    List<Venta> findByEstadoOrderByFechaVentaDesc(EstadoVenta estado);

    /**
     * Ventas entre fechas
     */
    List<Venta> findByFechaVentaBetweenOrderByFechaVentaDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Contar ventas por estado
     */
    Long countByEstado(EstadoVenta estado);

    /**
     * Ventas del día
     */
    @Query("SELECT v FROM Venta v WHERE CAST(v.fechaVenta AS date) = CAST(CURRENT_DATE AS date) ORDER BY v.fechaVenta DESC")
    List<Venta> findVentasDelDia();

    /**
     * Últimas ventas (límite)
     */
    List<Venta> findTop10ByOrderByFechaVentaDesc();

    /**
     * Total de ventas en un rango de fechas
     */
    @Query("SELECT SUM(v.montoTotal) FROM Venta v WHERE v.fechaVenta BETWEEN :inicio AND :fin AND v.estado = 'COMPLETADA'")
    BigDecimal sumMontoTotalByFechaVentaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Contar ventas con cliente rápido (sin cliente registrado)
     */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.cliente IS NULL AND v.nombreClienteDirecto IS NOT NULL")
    Long countVentasClienteRapido();

    /**
     * Contar ventas con cliente registrado
     */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.cliente IS NOT NULL")
    Long countVentasClienteRegistrado();

    /**
     * Listar todas las ventas con cliente rápido
     */
    @Query("SELECT v FROM Venta v WHERE v.cliente IS NULL AND v.nombreClienteDirecto IS NOT NULL ORDER BY v.fechaVenta DESC")
    List<Venta> findVentasClienteRapido();

    // Obtiene una venta por ID, cargando también su cliente en una sola consulta para evitar errores de lazy loading.
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente WHERE v.id = :id")
    Optional<Venta> findByIdWithCliente(@Param("id") Long id);
    /**
     * Buscar ventas por método de pago y rango de fechas
     */
    @Query("SELECT v FROM Venta v WHERE v.metodoPago = :metodoPago AND v.fechaVenta BETWEEN :inicio AND :fin ORDER BY v.fechaVenta DESC")
    List<Venta> findByMetodoPagoAndFechaVentaBetween(
        @Param("metodoPago") String metodoPago,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
}