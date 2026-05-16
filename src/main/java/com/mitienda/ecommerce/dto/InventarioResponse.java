package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Inventario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventarioResponse {

    private Long id;
    private Long idProducto;
    private String nombreProducto;
    private String skuProducto;
    private Integer cantidadDisponible;
    private Integer stockMinimo;
    private String ubicacion;
    private Boolean bajoStockMinimo;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public InventarioResponse(Inventario inventario) {
        this.id = inventario.getId();
        this.idProducto = inventario.getProducto().getId();
        this.nombreProducto = inventario.getProducto().getNombre();
        this.skuProducto = inventario.getProducto().getSku();
        this.cantidadDisponible = inventario.getCantidadDisponible();
        this.stockMinimo = inventario.getProducto().getStockMinimo();
        this.ubicacion = inventario.getUbicacion();
        this.bajoStockMinimo = inventario.estaBajoStockMinimo();
        this.fechaActualizacion = inventario.getFechaActualizacion();
    }
}