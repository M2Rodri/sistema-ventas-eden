package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.TipoComprobante;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear comprobantes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteRequest {

    @NotNull(message = "La venta es obligatoria")
    private Long idVenta;

    @NotNull(message = "El tipo de comprobante es obligatorio")
    private TipoComprobante tipoComprobante = TipoComprobante.RECIBO;

    @Size(max = 200, message = "El nombre del cliente no puede exceder 200 caracteres")
    private String nombreCliente;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}