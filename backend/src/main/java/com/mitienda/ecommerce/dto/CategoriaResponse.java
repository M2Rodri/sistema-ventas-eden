package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.models.TipoProducto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de categoría
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private TipoProducto tipoProducto;
    private Boolean activo;
    private Long cantidadProductos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad
    public CategoriaResponse(Categoria categoria) {
        this.id = categoria.getId();
        this.nombre = categoria.getNombre();
        this.descripcion = categoria.getDescripcion();
        this.tipoProducto = categoria.getTipoProducto();
        this.activo = categoria.getActivo();
        this.cantidadProductos = (long) categoria.getProductos().size();
        this.fechaCreacion = categoria.getFechaCreacion();
        this.fechaActualizacion = categoria.getFechaActualizacion();
    }
}