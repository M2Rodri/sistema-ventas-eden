package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de entrega que se pueden completar o corregir después de registrar
 * la venta (solo ADMIN).
 *
 * Un campo vacío o en blanco lo deja sin valor: así se puede borrar un dato
 * cargado por error. La transportadora y la guía solo aplican a ventas por
 * TRANSPORTADORA; en DOMICILIO solo se edita la dirección.
 */
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
