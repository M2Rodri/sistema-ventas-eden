package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad ImagenProducto - Múltiples imágenes por producto
 */
@Entity
@Table(name = "imagenes_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La URL es obligatoria")
    @Column(nullable = false, length = 500)
    private String urlImagen;

    @Column(nullable = false)
    private Boolean esPrincipal = false;

    @Column(nullable = false)
    private Integer orden = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Constructor personalizado
    public ImagenProducto(String urlImagen, Boolean esPrincipal, Producto producto) {
        this.urlImagen = urlImagen;
        this.esPrincipal = esPrincipal;
        this.producto = producto;
    }
}