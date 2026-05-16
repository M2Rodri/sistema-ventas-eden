package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Transportadora;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respuestas de transportadora
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportadoraResponse {

    private Long id;
    private String nombre;
    private String telefono;
    private String correo;
    private BigDecimal tarifaBase;
    private Integer tiempoEstimadoDias;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public TransportadoraResponse(Transportadora transportadora) {
        this.id = transportadora.getId();
        this.nombre = transportadora.getNombre();
        this.telefono = transportadora.getTelefono();
        this.correo = transportadora.getCorreo();
        this.tarifaBase = transportadora.getTarifaBase();
        this.tiempoEstimadoDias = transportadora.getTiempoEstimadoDias();
        this.activo = transportadora.getActivo();
        this.fechaRegistro = transportadora.getFechaRegistro();
        this.fechaActualizacion = transportadora.getFechaActualizacion();
    }
}