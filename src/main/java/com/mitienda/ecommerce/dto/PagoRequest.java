package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar un pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequest {

    @NotNull(message = "La venta es obligatoria")
    private Long idVenta;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referencia;

    /** Nota libre del cobro. */
    private String observacion;
}