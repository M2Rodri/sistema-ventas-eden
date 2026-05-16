package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para validar un cupón
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCuponRequest {

    @NotBlank(message = "El código del cupón es obligatorio")
    private String codigoCupon;

    @NotNull(message = "El monto de la compra es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    private BigDecimal montoCompra;

    private Long idCliente; // Para validar si es primera compra
}