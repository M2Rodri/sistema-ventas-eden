package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.DetalleVenta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para detalle de venta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVentaDTO {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    // Constructor desde entidad
    public DetalleVentaDTO(DetalleVenta detalle) {
        this.id = detalle.getId();
        this.idProducto = detalle.getProducto().getId();
        this.nombreProducto = detalle.getProducto().getNombre();
        this.skuProducto = detalle.getProducto().getSku();
        this.cantidad = detalle.getCantidad();
        this.precioUnitario = detalle.getPrecioUnitario();
        this.subtotal = detalle.getSubtotal();
    }
}