package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Transportadora - Empresas de envío
 */
@Entity
@Table(name = "transportadoras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transportadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 15)
    private String telefono;

    @Column(length = 100)
    private String correo;

    @DecimalMin(value = "0.0", message = "La tarifa base no puede ser negativa")
    @Column(precision = 10, scale = 2)
    private BigDecimal tarifaBase = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer tiempoEstimadoDias = 3; // Días estimados de entrega

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relación con envíos
    @OneToMany(mappedBy = "transportadora", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Envio> envios = new ArrayList<>();

    // Constructor personalizado
    public Transportadora(String nombre, BigDecimal tarifaBase, Integer tiempoEstimadoDias) {
        this.nombre = nombre;
        this.tarifaBase = tarifaBase;
        this.tiempoEstimadoDias = tiempoEstimadoDias;
        this.activo = true;
    }
}