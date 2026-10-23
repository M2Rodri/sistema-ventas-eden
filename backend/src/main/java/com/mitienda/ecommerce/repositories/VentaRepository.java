package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Consultas de ventas. */
public interface VentaRepository extends JpaRepository<Venta, Long> {
    /** Listar ventas por cliente registrado */
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);

    /** Ventas por nombre de cliente; la venta de mostrador también tiene cliente (INVITADO). */
    @Query("SELECT v FROM Venta v WHERE " +
           "LOWER(v.cliente.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
           "LOWER(v.cliente.apellido) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByNombreClienteContaining(@Param("nombre") String nombre);

    /** Buscar ventas por teléfono del cliente. */
    @Query("SELECT v FROM Venta v WHERE " +
           "v.cliente.telefono LIKE CONCAT('%', :telefono, '%') " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByTelefonoClienteContaining(@Param("telefono") String telefono);

    /** Filtrar ventas por estado */
    List<Venta> findByEstadoOrderByFechaVentaDesc(EstadoVenta estado);

    /** Ventas entre fechas */
    List<Venta> findByFechaVentaBetweenOrderByFechaVentaDesc(LocalDateTime inicio, LocalDateTime fin);

    /** Contar ventas por estado */
    Long countByEstado(EstadoVenta estado);

    /** Ventas del día */
    @Query("SELECT v FROM Venta v WHERE CAST(v.fechaVenta AS date) = CAST(CURRENT_DATE AS date) ORDER BY v.fechaVenta DESC")
    List<Venta> findVentasDelDia();

    /** Últimas ventas (límite) */
    List<Venta> findTop10ByOrderByFechaVentaDesc();

    /** Total de ventas en un rango de fechas */
    @Query("SELECT SUM(v.montoTotal) FROM Venta v WHERE v.fechaVenta BETWEEN :inicio AND :fin AND v.estado = 'COMPLETADA'")
    BigDecimal sumMontoTotalByFechaVentaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /** Cantidad de ventas, monto total y última compra por cliente en una sola consulta (evita N+1). */
    @Query("SELECT v.cliente.id, COUNT(v), COALESCE(SUM(v.montoTotal), 0), MAX(v.fechaVenta) " +
           "FROM Venta v GROUP BY v.cliente.id")
    List<Object[]> aggregarEstadisticasPorCliente();

    // Obtiene una venta por ID, cargando también su cliente en una sola consulta para evitar errores de lazy loading.
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente WHERE v.id = :id")
    Optional<Venta> findByIdWithCliente(@Param("id") Long id);
    /** Ventas con al menos un pago del método indicado; el método vive en 'pagos'. */
    @Query("SELECT DISTINCT v FROM Venta v JOIN v.pagos p " +
           "WHERE p.metodoPago = :metodoPago AND v.fechaVenta BETWEEN :inicio AND :fin " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByMetodoPagoAndFechaVentaBetween(
        @Param("metodoPago") MetodoPago metodoPago,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    /** Ventas por cobrar: quedó saldo pendiente o nunca se completó el pago. */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.saldoPendiente > 0 OR v.estado = com.mitienda.ecommerce.models.EstadoVenta.PENDIENTE_PAGO")
    Long countVentasPorCobrar();

    /** Monto total por cobrar de esas mismas ventas. */
    @Query("SELECT COALESCE(SUM(v.saldoPendiente), 0) FROM Venta v WHERE v.saldoPendiente > 0 OR v.estado = com.mitienda.ecommerce.models.EstadoVenta.PENDIENTE_PAGO")
    BigDecimal sumSaldoPendientePorCobrar();

    /** Ventas por entregar (entrega distinta de ENTREGADO y no cancelada); una sola definición para backend, web y app. */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.estadoEntrega <> com.mitienda.ecommerce.models.EstadoEntrega.ENTREGADO " +
           "AND v.estado <> com.mitienda.ecommerce.models.EstadoVenta.CANCELADA")
    Long countVentasPorEntregar();

    /** Cifras COMPLETADAS de hoy, mes y año en una consulta: [cantHoy, montoHoy, cantMes, montoMes, cantAnio, montoAnio]. */
    @Query("SELECT COUNT(CASE WHEN v.fechaVenta BETWEEN :hoy AND :finHoy THEN 1 END), "
            + "COALESCE(SUM(CASE WHEN v.fechaVenta BETWEEN :hoy AND :finHoy THEN v.montoTotal END), 0), "
            + "COUNT(CASE WHEN v.fechaVenta BETWEEN :inicioMes AND :finHoy THEN 1 END), "
            + "COALESCE(SUM(CASE WHEN v.fechaVenta BETWEEN :inicioMes AND :finHoy THEN v.montoTotal END), 0), "
            + "COUNT(v), COALESCE(SUM(v.montoTotal), 0) "
            + "FROM Venta v WHERE v.estado = com.mitienda.ecommerce.models.EstadoVenta.COMPLETADA "
            + "AND v.fechaVenta BETWEEN :inicioAnio AND :finHoy")
    List<Object[]> resumenVentasCompletadas(@Param("hoy") LocalDateTime hoy,
                                            @Param("finHoy") LocalDateTime finHoy,
                                            @Param("inicioMes") LocalDateTime inicioMes,
                                            @Param("inicioAnio") LocalDateTime inicioAnio);

    /** Cantidad y saldo por cobrar en una consulta: [cantidad, saldo]. */
    @Query("SELECT COUNT(v), COALESCE(SUM(v.saldoPendiente), 0) FROM Venta v "
            + "WHERE v.saldoPendiente > 0 OR v.estado = com.mitienda.ecommerce.models.EstadoVenta.PENDIENTE_PAGO")
    List<Object[]> resumenPorCobrar();

    /** Fecha, estado y monto de las ventas de un rango, sin armar las ventas completas. */
    @Query("SELECT v.fechaVenta, v.estado, v.montoTotal FROM Venta v WHERE v.fechaVenta BETWEEN :inicio AND :fin")
    List<Object[]> findResumenVentasEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
