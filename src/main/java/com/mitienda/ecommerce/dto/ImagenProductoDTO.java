package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.ImagenProducto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para imágenes de productos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenProductoDTO {

    private Long id;
    private String urlImagen;
    private Boolean esPrincipal;
    private Integer orden;

    // Constructor desde entidad
    public ImagenProductoDTO(ImagenProducto imagen) {
        this.id = imagen.getId();
        this.urlImagen = imagen.getUrlImagen();
        this.esPrincipal = imagen.getEsPrincipal();
        this.orden = imagen.getOrden();
    }
}