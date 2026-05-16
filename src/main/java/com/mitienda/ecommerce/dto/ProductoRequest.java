package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.TipoProducto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar productos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede exceder 2000 caracteres")
    private String descripcion;

    @Size(max = 100, message = "El modelo no puede exceder 100 caracteres")
    private String modelo;

    @NotNull(message = "La categoría es obligatoria")
    private Long idCategoria;

    @Size(max = 50, message = "La calidad no puede exceder 50 caracteres")
    private String calidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio de venta debe ser mayor a 0")
    private BigDecimal precioVenta;

    @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
    private BigDecimal peso;

    @Size(max = 100, message = "Las dimensiones no pueden exceder 100 caracteres")
    private String dimensiones;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo = 0;

    @NotNull(message = "El tipo de producto es obligatorio")
    private TipoProducto tipoProducto;

    @NotNull(message = "El estado es obligatorio")
    private Boolean activo = true;
}