package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para operaciones con compras
 */
public interface CompraRepository extends JpaRepository<Compra, Long> {

    /**
     * Para el aviso amable de factura repetida: la base ya lo bloquea
     * (uq_compras_proveedor_factura), esto es para avisar antes de intentar
     * guardar, en vez de que el usuario vea el error técnico de la base.
     */
    boolean existsByProveedorIdAndNumeroFactura(Long proveedorId, String numeroFactura);

    /**
     * Mismo aviso, pero al editar: hay que ignorar la propia compra que se
     * está editando (si no cambió el número, no es un choque con otra).
     */
    boolean existsByProveedorIdAndNumeroFacturaAndIdNot(Long proveedorId, String numeroFactura, Long id);

    /**
     * Compras entre fechas. Lo usa ReporteService para el reporte financiero
     * (ingresos por ventas contra gastos en compras).
     */
    List<Compra> findByFechaCompraBetweenOrderByFechaCompraDesc(LocalDateTime inicio, LocalDateTime fin);
}