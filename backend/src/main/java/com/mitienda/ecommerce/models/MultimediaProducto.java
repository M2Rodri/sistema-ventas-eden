package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad MultimediaProducto - Modelos 3D y Realidad Aumentada
 */
@Entity
@Table(name = "multimedia_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultimediaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Size(max = 500, message = "La URL del modelo 3D no puede exceder 500 caracteres")
    @Column(length = 500)
    private String urlModelo3d;

    @Size(max = 500, message = "La URL de vista previa no puede exceder 500 caracteres")
    @Column(length = 500)
    private String urlVistaPrevia;

    @Column(nullable = false)
    private Boolean habilitadoRa = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Constructor personalizado
    public MultimediaProducto(Producto producto, String urlModelo3d, String urlVistaPrevia, Boolean habilitadoRa) {
        this.producto = producto;
        this.urlModelo3d = urlModelo3d;
        this.urlVistaPrevia = urlVistaPrevia;
        this.habilitadoRa = habilitadoRa;
        this.activo = true;
    }
}