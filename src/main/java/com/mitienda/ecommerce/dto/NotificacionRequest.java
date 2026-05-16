package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.TipoNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear notificaciones manualmente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionRequest {
    
    private Long usuarioId; // null = notificación global
    
    @NotNull(message = "El tipo de notificación es obligatorio")
    private TipoNotificacion tipo;
    
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede exceder 100 caracteres")
    private String titulo;
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
    private String mensaje;
    
    @Size(max = 200, message = "El enlace no puede exceder 200 caracteres")
    private String enlace;
}