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
    private BigDecimal precioUnitarioOriginal;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    /** Costo unitario histórico guardado al momento de vender; null si no tiene. */
    private BigDecimal costoUnitario;

    // Constructor desde entidad
    /** Sin el costo: es lo que ve un Empleado. Para incluirlo hay que pedirlo (solo el Admin). */
    public DetalleVentaDTO(DetalleVenta detalle) {
        this(detalle, false);
    }

    /** @param incluirCosto true solo para el Admin: el costo no debe llegar a un Empleado en ninguna respuesta. */
    public DetalleVentaDTO(DetalleVenta detalle, boolean incluirCosto) {
        this.id = detalle.getId();
        this.idProducto = detalle.getProducto().getId();
        this.nombreProducto = detalle.getProducto().getNombre();
        this.skuProducto = detalle.getProducto().getSku();
        this.cantidad = detalle.getCantidad();
        this.precioUnitarioOriginal = detalle.getPrecioUnitarioOriginal() != null
                ? detalle.getPrecioUnitarioOriginal()
                : detalle.getPrecioUnitario();
        this.precioUnitario = detalle.getPrecioUnitario();
        this.subtotal = detalle.getSubtotal();
        this.costoUnitario = incluirCosto ? detalle.getCostoUnitario() : null;
    }
}