package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de notificaciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    
    private Long id;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    private String enlace;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLeida;
    
    // Datos del usuario (si aplica)
    private Long usuarioId;
    private String usuarioNombre;
}