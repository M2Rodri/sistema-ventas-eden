package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.DetalleCompra;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para detalle de compra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraDTO {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    // Constructor desde entidad
    public DetalleCompraDTO(DetalleCompra detalle) {
        this.id = detalle.getId();
        this.idProducto = detalle.getProducto().getId();
        this.nombreProducto = detalle.getProducto().getNombre();
        this.skuProducto = detalle.getProducto().getSku();
        this.cantidad = detalle.getCantidad();
        this.precioUnitario = detalle.getPrecioUnitario();
        this.subtotal = detalle.getSubtotal();
    }
}