package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.TipoCupon;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para crear o actualizar cupones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuponRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 3, max = 50, message = "El código debe tener entre 3 y 50 caracteres")
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo de cupón es obligatorio")
    private TipoCupon tipoCupon;

    @NotNull(message = "El valor del descuento es obligatorio")
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal valorDescuento;

    @DecimalMin(value = "0.0", message = "El monto mínimo no puede ser negativo")
    private BigDecimal montoMinimo = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "El descuento máximo no puede ser negativo")
    private BigDecimal descuentoMaximo;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @Min(value = 1, message = "El uso máximo debe ser al menos 1")
    private Integer usoMaximo;

    private Boolean activo = true;

    private Boolean primeraCompra = false;
}