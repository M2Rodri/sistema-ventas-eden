package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.Notificacion;
import com.mitienda.ecommerce.models.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para notificaciones
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Notificaciones de un usuario (ordenadas por fecha)
     */
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    /**
     * Notificaciones no leídas de un usuario
     */
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(Long usuarioId);

    /**
     * Notificaciones globales (sin usuario específico)
     */
    List<Notificacion> findByUsuarioIsNullOrderByFechaCreacionDesc();

    /**
     * Notificaciones no leídas globales
     */
    List<Notificacion> findByUsuarioIsNullAndLeidaFalseOrderByFechaCreacionDesc();

    /**
     * Notificaciones por tipo
     */
    List<Notificacion> findByTipoOrderByFechaCreacionDesc(TipoNotificacion tipo);

    /**
     * Contar notificaciones no leídas de un usuario
     */
    Long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    /**
     * Contar notificaciones no leídas globales
     */
    Long countByUsuarioIsNullAndLeidaFalse();

    /**
     * Notificaciones antiguas (para limpieza)
     */
    @Query("SELECT n FROM Notificacion n WHERE n.leida = true AND n.fechaLeida < :fecha")
    List<Notificacion> findNotificacionesAntiguasLeidas(LocalDateTime fecha);
}