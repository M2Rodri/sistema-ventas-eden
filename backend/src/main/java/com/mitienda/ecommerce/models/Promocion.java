package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Promocion - Promociones y descuentos
 */
@Entity
@Table(name = "promociones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la promoción ("Liquidación de invierno"). Es NOT NULL en la
     * base y la entidad no lo mapeaba: sin él, todo INSERT fallaba.
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    /** En la base es de tipo text, sin límite de longitud. */
    @Column(columnDefinition = "text")
    private String descripcion;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede ser mayor a 100%")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal descuento; // Porcentaje de descuento

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relación con productos en promocion
    @ManyToMany
    @JoinTable(
        name = "promociones_producto",
        joinColumns = @JoinColumn(name = "id_promocion"),
        inverseJoinColumns = @JoinColumn(name = "id_producto")
    )
    private List<Producto> productos = new ArrayList<>();

    // Constructor personalizado
    public Promocion(String descripcion, BigDecimal descuento, LocalDate fechaInicio, LocalDate fechaFin) {
        this.descripcion = descripcion;
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = true;
    }

    /**
     * Verificar si la promocion está vigente
     */
    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        return this.activo && 
               !hoy.isBefore(this.fechaInicio) && 
               !hoy.isAfter(this.fechaFin);
    }
}