package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.NotificacionDTO;
import com.mitienda.ecommerce.dto.NotificacionRequest;
import com.mitienda.ecommerce.models.Notificacion;
import com.mitienda.ecommerce.models.TipoNotificacion;
import com.mitienda.ecommerce.models.User;
import com.mitienda.ecommerce.repositories.NotificacionRepository;
import com.mitienda.ecommerce.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Notificaciones
 */
@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Crear notificación para un usuario específico
     */
    @Transactional
    public Notificacion crearNotificacion(Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje, String enlace) {
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Notificacion notificacion = new Notificacion(usuario, tipo, titulo, mensaje, enlace);
        return notificacionRepository.save(notificacion);
    }

    /**
     * Crear notificación global (para todos los admins)
     */
    @Transactional
    public Notificacion crearNotificacionGlobal(TipoNotificacion tipo, String titulo, String mensaje, String enlace) {
        Notificacion notificacion = new Notificacion(null, tipo, titulo, mensaje, enlace);
        return notificacionRepository.save(notificacion);
    }

    /**
     * Obtener todas las notificaciones de un usuario (propias + globales)
     */
    public List<Notificacion> obtenerNotificacionesUsuario(Long usuarioId) {
        List<Notificacion> notificaciones = new ArrayList<>();
        
        // Notificaciones propias
        notificaciones.addAll(notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId));
        
        // Notificaciones globales
        notificaciones.addAll(notificacionRepository.findByUsuarioIsNullOrderByFechaCreacionDesc());
        
        return notificaciones;
    }

    /**
     * Obtener notificaciones no leídas de un usuario
     */
    public List<Notificacion> obtenerNotificacionesNoLeidas(Long usuarioId) {
        List<Notificacion> notificaciones = new ArrayList<>();
        
        notificaciones.addAll(notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId));
        notificaciones.addAll(notificacionRepository.findByUsuarioIsNullAndLeidaFalseOrderByFechaCreacionDesc());
        
        return notificaciones;
    }

    /**
     * Contar notificaciones no leídas de un usuario
     */
    public Long contarNoLeidas(Long usuarioId) {
        Long propias = notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
        Long globales = notificacionRepository.countByUsuarioIsNullAndLeidaFalse();
        return propias + globales;
    }

    /**
     * Marcar notificación como leída
     */
    @Transactional
    public void marcarComoLeida(Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        
        if (!notificacion.getLeida()) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacionRepository.save(notificacion);
        }
    }

    /**
     * Marcar todas como leídas
     */
    @Transactional
    public void marcarTodasComoLeidas(Long usuarioId) {
        List<Notificacion> notificaciones = obtenerNotificacionesNoLeidas(usuarioId);
        
        for (Notificacion notificacion : notificaciones) {
            if (notificacion.getUsuario() == null || notificacion.getUsuario().getId().equals(usuarioId)) {
                notificacion.setLeida(true);
                notificacion.setFechaLeida(LocalDateTime.now());
            }
        }
        
        notificacionRepository.saveAll(notificaciones);
    }

    /**
     * Eliminar notificación
     */
    @Transactional
    public void eliminarNotificacion(Long notificacionId) {
        notificacionRepository.deleteById(notificacionId);
    }

    /**
     * Limpiar notificaciones antiguas (más de 30 días leídas)
     */
    @Transactional
    public void limpiarNotificacionesAntiguas() {
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        List<Notificacion> antiguas = notificacionRepository.findNotificacionesAntiguasLeidas(hace30Dias);
        notificacionRepository.deleteAll(antiguas);
    }

    /**
     * Notificar stock bajo (automático)
     */
    @Transactional
    public void notificarStockBajo(String nombreProducto, int cantidadActual) {
        String titulo = "⚠️ Stock Bajo";
        String mensaje = String.format("El producto '%s' tiene stock bajo (%d unidades)", nombreProducto, cantidadActual);
        String enlace = "/dashboard/inventario";
        
        crearNotificacionGlobal(TipoNotificacion.STOCK_BAJO, titulo, mensaje, enlace);
    }

    /**
     * Notificar nuevo pedido
     */
    @Transactional
    public void notificarNuevoPedido(Long pedidoId, String clienteNombre) {
        String titulo = "🛒 Nuevo Pedido";
        String mensaje = String.format("Pedido #%d de %s", pedidoId, clienteNombre);
        String enlace = String.format("/dashboard/pedidos/%d", pedidoId);
        
        crearNotificacionGlobal(TipoNotificacion.PEDIDO_NUEVO, titulo, mensaje, enlace);
    }

    /**
     * Notificar pago recibido
     */
    @Transactional
    public void notificarPagoRecibido(Long ventaId, Double monto) {
        String titulo = "💰 Pago Recibido";
        String mensaje = String.format("Pago de Bs. %.2f recibido para venta #%d", monto, ventaId);
        String enlace = String.format("/dashboard/ventas/%d", ventaId);
        
        crearNotificacionGlobal(TipoNotificacion.PAGO_RECIBIDO, titulo, mensaje, enlace);
    }

    /**
     * Convertir Notificacion a DTO
     */
    public NotificacionDTO convertirADTO(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setTipo(notificacion.getTipo());
        dto.setTitulo(notificacion.getTitulo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setEnlace(notificacion.getEnlace());
        dto.setLeida(notificacion.getLeida());
        dto.setFechaCreacion(notificacion.getFechaCreacion());
        dto.setFechaLeida(notificacion.getFechaLeida());
        
        if (notificacion.getUsuario() != null) {
            dto.setUsuarioId(notificacion.getUsuario().getId());
            dto.setUsuarioNombre(notificacion.getUsuario().getNombreCompleto());
        }
        
        return dto;
    }

    /**
     * Crear notificación desde DTO
     */
    @Transactional
    public Notificacion crearDesdeDTO(NotificacionRequest request) {
        if (request.getUsuarioId() != null) {
            return crearNotificacion(
                request.getUsuarioId(),
                request.getTipo(),
                request.getTitulo(),
                request.getMensaje(),
                request.getEnlace()
            );
        } else {
            return crearNotificacionGlobal(
                request.getTipo(),
                request.getTitulo(),
                request.getMensaje(),
                request.getEnlace()
            );
        }
    }
}