package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Inventario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Producto activo con precio y stock juntos, para GET /api/inventario/catalogo.
 *
 * Antes la app móvil pedía /api/productos/activos y /api/inventario por
 * separado y los cruzaba en el cliente. Este DTO es ese cruce hecho en el
 * servidor, en una sola consulta (ver InventarioRepository#findCatalogoApp).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoProductoResponse {

    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private Long idCategoria;
    private String nombreCategoria;
    private BigDecimal precioVenta;
    private List<ImagenProductoDTO> imagenes;
    private Integer cantidadDisponible;
    private Integer stockMinimo;
    private Boolean bajoStockMinimo;

    public CatalogoProductoResponse(Inventario inventario) {
        var producto = inventario.getProducto();
        this.id = producto.getId();
        this.sku = producto.getSku();
        this.nombre = producto.getNombre();
        this.descripcion = producto.getDescripcion();
        this.idCategoria = producto.getCategoria() != null ? producto.getCategoria().getId() : null;
        this.nombreCategoria = producto.getCategoria() != null ? producto.getCategoria().getNombre() : null;
        this.precioVenta = producto.getPrecioVenta();
        this.imagenes = producto.getImagenes().stream()
                .map(ImagenProductoDTO::new)
                .collect(Collectors.toList());
        this.cantidadDisponible = inventario.getCantidadDisponible();
        this.stockMinimo = producto.getStockMinimo();
        this.bajoStockMinimo = inventario.estaBajoStockMinimo();
    }
}
