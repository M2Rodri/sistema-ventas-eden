package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para operaciones con pagos
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    /**
     * Obtener pagos de una venta
     */
    List<Pago> findByVentaIdOrderByFechaPagoDesc(Long ventaId);

    /**
     * Filtrar pagos por estado
     */
    List<Pago> findByEstadoOrderByFechaPagoDesc(EstadoPago estado);

    /**
     * Contar pagos por estado
     */
    Long countByEstado(EstadoPago estado);
}