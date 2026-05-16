package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear o actualizar configuraciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionRequest {

    @NotBlank(message = "La clave es obligatoria")
    @Size(max = 100, message = "La clave no puede exceder 100 caracteres")
    private String clave;

    @NotBlank(message = "El valor es obligatorio")
    @Size(max = 500, message = "El valor no puede exceder 500 caracteres")
    private String valor;

    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcion;
}