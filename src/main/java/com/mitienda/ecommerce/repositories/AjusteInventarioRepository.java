package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.AjusteInventario;
import com.mitienda.ecommerce.models.TipoAjuste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para ajustes de inventario
 */
@Repository
public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {

    /**
     * Historial de ajustes de un producto
     */
    List<AjusteInventario> findByProductoIdOrderByFechaDesc(Long productoId);

    /**
     * Ajustes realizados por un usuario
     */
    List<AjusteInventario> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /**
     * Ajustes por tipo
     */
    List<AjusteInventario> findByTipoAjusteOrderByFechaDesc(String tipoAjuste);

    /**
     * Ajustes entre fechas
     */
    List<AjusteInventario> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Últimos ajustes
     */
    List<AjusteInventario> findTop50ByOrderByFechaDesc();
}