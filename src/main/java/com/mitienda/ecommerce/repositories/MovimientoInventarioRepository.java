package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.MovimientoInventario;
import com.mitienda.ecommerce.models.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para ajustes de inventario
 */
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /**
     * Historial de ajustes de un producto
     */
    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);

    /**
     * Ajustes realizados por un usuario
     */
    List<MovimientoInventario> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /**
     * Ajustes por tipo
     */
    List<MovimientoInventario> findByTipoMovimientoOrderByFechaDesc(String tipoMovimiento);

    /**
     * Ajustes entre fechas
     */
    List<MovimientoInventario> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Últimos ajustes
     */
    List<MovimientoInventario> findTop50ByOrderByFechaDesc();
}