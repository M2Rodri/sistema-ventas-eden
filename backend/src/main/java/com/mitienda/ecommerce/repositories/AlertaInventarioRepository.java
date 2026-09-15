package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.AlertaInventario;
import com.mitienda.ecommerce.models.EstadoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositorio para alertas de inventario
 */
public interface AlertaInventarioRepository extends JpaRepository<AlertaInventario, Long> {

    /**
     * Listar alertas por estado
     */
    List<AlertaInventario> findByEstadoOrderByFechaAlertaDesc(EstadoAlerta estado);

    /**
     * Alertas pendientes
     */
    @Query("SELECT a FROM AlertaInventario a WHERE a.estado = 'PENDIENTE' ORDER BY a.fechaAlerta DESC")
    List<AlertaInventario> findByEstadoPendienteOrderByFechaAlertaDesc();

    /**
     * Contar alertas por estado
     */
    Long countByEstado(EstadoAlerta estado);
}