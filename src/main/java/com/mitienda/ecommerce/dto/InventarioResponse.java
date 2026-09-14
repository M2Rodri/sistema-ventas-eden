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

    /**
     * Categoría real del producto. Antes no viajaba en la respuesta y la
     * pantalla la adivinaba buscando texto en el nombre ("colchon"), lo que
     * fallaba con las tildes y mostraba "Otro" en todos los colchones.
     */
    private Long idCategoria;
    private String nombreCategoria;

    /** Costo de referencia del producto: permite valorizar el inventario. */
    private java.math.BigDecimal costoReferencial;
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
        this.idCategoria = inventario.getProducto().getCategoria() != null
                ? inventario.getProducto().getCategoria().getId() : null;
        this.nombreCategoria = inventario.getProducto().getCategoria() != null
                ? inventario.getProducto().getCategoria().getNombre() : null;
        this.costoReferencial = inventario.getProducto().getCostoReferencial();
        this.cantidadDisponible = inventario.getCantidadDisponible();
        this.stockMinimo = inventario.getProducto().getStockMinimo();
        this.ubicacion = inventario.getUbicacion();
        this.bajoStockMinimo = inventario.estaBajoStockMinimo();
        this.fechaActualizacion = inventario.getFechaActualizacion();
    }
}