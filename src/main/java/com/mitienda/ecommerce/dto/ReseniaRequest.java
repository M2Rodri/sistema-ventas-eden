package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear reseñas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReseniaRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long idProducto;

    @NotNull(message = "El cliente es obligatorio")
    private Long idCliente;

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer calificacion;

    @Size(max = 1000, message = "El comentario no puede exceder 1000 caracteres")
    private String comentario;
}