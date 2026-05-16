package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Resenia - Reseñas y calificaciones de productos
 */
@Entity
@Table(name = "resenias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resenia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    @Column(nullable = false)
    private Integer calificacion;

    @Size(max = 1000, message = "El comentario no puede exceder 1000 caracteres")
    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false)
    private Boolean aprobado = false; // Requiere aprobación de admin

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    // Constructor personalizado
    public Resenia(Producto producto, Cliente cliente, Integer calificacion, String comentario) {
        this.producto = producto;
        this.cliente = cliente;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.aprobado = false;
    }
}