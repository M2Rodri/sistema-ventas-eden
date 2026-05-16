package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.ConfiguracionSistema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de configuración
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionResponse {

    private Long id;
    private String clave;
    private String valor;
    private String descripcion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public ConfiguracionResponse(ConfiguracionSistema config) {
        this.id = config.getId();
        this.clave = config.getClave();
        this.valor = config.getValor();
        this.descripcion = config.getDescripcion();
        this.fechaActualizacion = config.getFechaActualizacion();
    }
}