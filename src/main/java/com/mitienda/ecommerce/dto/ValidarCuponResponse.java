package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para respuesta de validación de cupón
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCuponResponse {

    private Boolean valido;
    private String mensaje;
    private BigDecimal descuentoAplicado;
    private BigDecimal montoFinal;
    private Boolean envioGratis;
}