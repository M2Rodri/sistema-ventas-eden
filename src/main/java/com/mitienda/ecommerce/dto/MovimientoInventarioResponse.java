package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.MovimientoInventario;
import com.mitienda.ecommerce.models.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de movimiento de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventarioResponse {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private Integer cantidadAnterior;
    private Integer cantidadNueva;
    private Integer cantidad;
    private String tipoMovimiento;
    private String motivo;
    private String observacion;
    private Long idUsuario;
    private String nombreUsuario;
    private LocalDateTime fecha;

    // Constructor desde entidad
    public MovimientoInventarioResponse(MovimientoInventario movimiento) {
        this.id = movimiento.getId();
        this.idProducto = movimiento.getProducto().getId();
        this.nombreProducto = movimiento.getProducto().getNombre();
        this.skuProducto = movimiento.getProducto().getSku();
        this.cantidadAnterior = movimiento.getCantidadAnterior();
        this.cantidadNueva = movimiento.getCantidadNueva();
        this.cantidad = movimiento.getCantidad();
        this.tipoMovimiento = movimiento.getTipoMovimiento();
        this.motivo = movimiento.getMotivo();
        this.observacion = movimiento.getObservacion();
        this.idUsuario = movimiento.getUsuario() != null ? movimiento.getUsuario().getId() : null;
        this.nombreUsuario = movimiento.getUsuario() != null ? movimiento.getUsuario().getNombreCompleto() : "Sistema";
        this.fecha = movimiento.getFecha();
    }
}