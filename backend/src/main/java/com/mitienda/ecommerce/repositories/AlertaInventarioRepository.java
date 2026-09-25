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

    /**
     * Si ya existe una alerta pendiente para el producto, antes de crear una
     * nueva. Sin este chequeo, cada ajuste/venta que deja al producto igual
     * de bajo genera una alerta nueva, y se apilan varias pendientes para
     * el mismo producto sin que nadie note que ya había una.
     */
    boolean existsByProductoIdAndEstado(Long idProducto, EstadoAlerta estado);

    /**
     * Alertas pendientes de un producto, para resolverlas solas cuando el
     * stock ya se repuso (ver InventarioService#resolverAlertasPendientes).
     */
    List<AlertaInventario> findByProductoIdAndEstado(Long idProducto, EstadoAlerta estado);
}