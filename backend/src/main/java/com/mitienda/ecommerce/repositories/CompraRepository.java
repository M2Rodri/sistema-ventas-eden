package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Repositorio para operaciones con compras */
public interface CompraRepository extends JpaRepository<Compra, Long> {
    /** Para avisar de una factura repetida antes de que la base la rechace (uq_compras_proveedor_factura). */
    boolean existsByProveedorIdAndNumeroFactura(Long proveedorId, String numeroFactura);

    /** Igual al editar, ignorando la propia compra. */
    boolean existsByProveedorIdAndNumeroFacturaAndIdNot(Long proveedorId, String numeroFactura, Long id);

    /** Compras entre fechas, para el reporte financiero. */
    List<Compra> findByFechaCompraBetweenOrderByFechaCompraDesc(LocalDateTime inicio, LocalDateTime fin);
}