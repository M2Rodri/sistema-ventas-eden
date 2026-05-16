package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para ajustes de inventario (entrada/salida manual)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjusteInventarioRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long idProducto;

    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;

    @NotBlank(message = "El tipo de ajuste es obligatorio")
    private String tipoAjuste; // "ENTRADA" o "SALIDA"

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 200, message = "El motivo no puede exceder 200 caracteres")
    private String motivo;
}