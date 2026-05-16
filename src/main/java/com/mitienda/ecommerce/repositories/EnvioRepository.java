package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Envio;
import com.mitienda.ecommerce.models.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con envíos
 */
@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long> {

    /**
     * Buscar envío por venta
     */
    Optional<Envio> findByVentaId(Long ventaId);

    /**
     * Buscar envío por guía de remisión
     */
    Optional<Envio> findByGuiaRemision(String guiaRemision);

    /**
     * Filtrar envíos por estado
     */
    List<Envio> findByEstadoSeguimientoOrderByFechaCreacionDesc(EstadoEnvio estado);

    /**
     * Envíos por transportadora
     */
    List<Envio> findByTransportadoraIdOrderByFechaCreacionDesc(Long transportadoraId);

    /**
     * Envíos pendientes
     */
    @Query("SELECT e FROM Envio e WHERE e.estadoSeguimiento = 'PENDIENTE' ORDER BY e.fechaCreacion DESC")
    List<Envio> findEnviosPendientes();

    /**
     * Envíos en camino
     */
    @Query("SELECT e FROM Envio e WHERE e.estadoSeguimiento = 'EN_CAMINO' ORDER BY e.fechaEntregaEstimada ASC")
    List<Envio> findEnviosEnCamino();

    /**
     * Envíos con entrega estimada para hoy o atrasados
     */
    @Query("SELECT e FROM Envio e WHERE e.fechaEntregaEstimada <= :fecha AND e.estadoSeguimiento NOT IN ('ENTREGADO', 'CANCELADO', 'DEVUELTO')")
    List<Envio> findEnviosPorEntregar(LocalDate fecha);

    /**
     * Contar envíos por estado
     */
    Long countByEstadoSeguimiento(EstadoEnvio estado);
}