package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para auditorías
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /**
     * Auditorías de un usuario
     */
    List<Auditoria> findByUsuarioIdOrderByFechaHoraDesc(Long usuarioId);

    /**
     * Auditorías de una tabla específica
     */
    List<Auditoria> findByTablaAfectadaOrderByFechaHoraDesc(String tablaAfectada);

    /**
     * Auditorías de una acción específica
     */
    List<Auditoria> findByAccionOrderByFechaHoraDesc(String accion);

    /**
     * Auditorías entre fechas
     */
    List<Auditoria> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Últimas auditorías
     */
    List<Auditoria> findTop100ByOrderByFechaHoraDesc();

    /**
     * Auditorías del día
     */
    @Query("SELECT a FROM Auditoria a WHERE CAST(a.fechaHora AS date) = CAST(CURRENT_DATE AS date) ORDER BY a.fechaHora DESC")
    List<Auditoria> findAuditoriasDelDia();
}