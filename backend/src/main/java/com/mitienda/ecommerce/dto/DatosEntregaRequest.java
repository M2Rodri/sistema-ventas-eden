package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos de entrega a completar o corregir tras la venta (solo ADMIN); un campo vacío borra el dato. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosEntregaRequest {
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    private String direccionDestino;

    @Size(max = 100, message = "El nombre de la transportadora no puede exceder 100 caracteres")
    private String transportadora;

    @Size(max = 100, message = "La guía de remisión no puede exceder 100 caracteres")
    private String guiaRemision;
}
