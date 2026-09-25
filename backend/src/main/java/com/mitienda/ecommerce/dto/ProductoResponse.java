package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.TipoProducto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuestas de producto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {

    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private String modelo;
    private String marca;
    private String firmeza;
    private String materialNucleo;
    private String color;
    private String materialArmazon;
    private Long idCategoria;
    private String nombreCategoria;
    private String calidad;
    private BigDecimal precioCompra;
    // Si es true, precioCompra ya lo gobierna una compra confirmada y no se
    // puede editar a mano en Productos. Si es false, sigue siendo una
    // estimación inicial y se puede corregir.
    private Boolean tieneComprasConfirmadas;
    private BigDecimal precioVenta;
    private String dimensiones;
    private Integer stockMinimo;
    private TipoProducto tipoProducto;
    private Boolean activo;
    private List<ImagenProductoDTO> imagenes; // Asegúrate que ImagenProductoDTO esté correctamente definido
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public ProductoResponse(Producto producto) {
        this.id = producto.getId();
        this.sku = producto.getSku();
        this.nombre = producto.getNombre();
        this.descripcion = producto.getDescripcion();
        this.modelo = producto.getModelo();
        this.marca = producto.getMarca();
        this.firmeza = producto.getFirmeza();
        this.materialNucleo = producto.getMaterialNucleo();
        this.color = producto.getColor();
        this.materialArmazon = producto.getMaterialArmazon();
        this.idCategoria = producto.getCategoria().getId();
        this.nombreCategoria = producto.getCategoria().getNombre();
        this.calidad = producto.getCalidad();
        this.precioCompra = producto.getPrecioCompra();
        this.tieneComprasConfirmadas = false; // lo completa ProductoService, que sí sabe consultar Compras
        this.precioVenta = producto.getPrecioVenta();
        this.dimensiones = producto.getDimensiones();
        this.stockMinimo = producto.getStockMinimo();
        this.tipoProducto = producto.getTipoProducto();
        this.activo = producto.getActivo();
        // Mapeo de la lista de entidades ImagenProducto a DTOs ImagenProductoDTO
        this.imagenes = producto.getImagenes().stream()
                .map(ImagenProductoDTO::new) // Asegúrate que ImagenProductoDTO tenga un constructor ImagenProductoDTO(ImagenProducto entity)
                .collect(Collectors.toList());
        this.fechaCreacion = producto.getFechaCreacion();
        this.fechaActualizacion = producto.getFechaActualizacion();
    }
}