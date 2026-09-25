package com.mitienda.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para crear compra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private Long idProveedor;

    @Size(max = 50, message = "El número de factura no puede exceder 50 caracteres")
    private String numeroFactura;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notas;

    @NotNull(message = "Los productos son obligatorios")
    @Size(min = 1, message = "Debe incluir al menos un producto")
    @Valid
    private List<ItemCompraRequest> items;

    /**
     * Clase interna para items de la compra
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemCompraRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        private Long idProducto;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
        private BigDecimal precioUnitario;
    }
}