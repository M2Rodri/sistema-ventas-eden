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

/**
 * Repositorio para operaciones con ventas
 * Soporta búsquedas tanto por cliente registrado como cliente directo
 */
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Listar ventas por cliente registrado
     */
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);

    /**
     * Buscar ventas por nombre de cliente.
     * Ya no hace falta buscar en dos lugares: la venta de mostrador también
     * tiene su cliente (tipo INVITADO), así que basta con mirar 'clientes'.
     */
    @Query("SELECT v FROM Venta v WHERE " +
           "LOWER(v.cliente.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
           "LOWER(v.cliente.apellido) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByNombreClienteContaining(@Param("nombre") String nombre);

    /**
     * Buscar ventas por teléfono del cliente.
     */
    @Query("SELECT v FROM Venta v WHERE " +
           "v.cliente.telefono LIKE CONCAT('%', :telefono, '%') " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByTelefonoClienteContaining(@Param("telefono") String telefono);

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
     * Contar ventas de mostrador (cliente cargado como INVITADO).
     * Antes se distinguían por tener nombreClienteDirecto y ningún cliente;
     * ahora la venta de mostrador sí tiene cliente, marcado como INVITADO.
     */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.cliente.tipoCliente = com.mitienda.ecommerce.models.TipoCliente.INVITADO")
    Long countVentasClienteRapido();

    /**
     * Contar ventas de clientes con datos completos.
     */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.cliente.tipoCliente = com.mitienda.ecommerce.models.TipoCliente.REGISTRADO")
    Long countVentasClienteRegistrado();

    /**
     * Listar las ventas de mostrador.
     */
    @Query("SELECT v FROM Venta v WHERE v.cliente.tipoCliente = com.mitienda.ecommerce.models.TipoCliente.INVITADO ORDER BY v.fechaVenta DESC")
    List<Venta> findVentasClienteRapido();

    // Obtiene una venta por ID, cargando también su cliente en una sola consulta para evitar errores de lazy loading.
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente WHERE v.id = :id")
    Optional<Venta> findByIdWithCliente(@Param("id") Long id);
    /**
     * Buscar ventas que tengan al menos un pago con el método indicado.
     *
     * El método de pago dejó de ser un campo de 'ventas' y vive en 'pagos',
     * porque una venta admite varios cobros con métodos distintos. Por eso la
     * consulta se hace por EXISTS sobre los pagos en lugar de comparar un
     * campo de la venta.
     */
    @Query("SELECT DISTINCT v FROM Venta v JOIN v.pagos p " +
           "WHERE p.metodoPago = :metodoPago AND v.fechaVenta BETWEEN :inicio AND :fin " +
           "ORDER BY v.fechaVenta DESC")
    List<Venta> findByMetodoPagoAndFechaVentaBetween(
        @Param("metodoPago") MetodoPago metodoPago,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
}