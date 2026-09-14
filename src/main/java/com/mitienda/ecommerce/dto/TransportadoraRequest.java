package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar transportadoras
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportadoraRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres")
    private String telefono;

    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @DecimalMin(value = "0.0", message = "La tarifa base no puede ser negativa")
    private BigDecimal tarifaBase = BigDecimal.ZERO;

    @Min(value = 1, message = "El tiempo estimado debe ser al menos 1 día")
    private Integer tiempoEstimadoDias = 3;

    private Boolean activo = true;
}