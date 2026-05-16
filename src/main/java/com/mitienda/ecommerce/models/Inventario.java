package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Inventario - Control de stock de productos
 */
@Entity
@Table(name = "inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false, unique = true)
    private Producto producto;

    @NotNull(message = "La cantidad disponible es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    private Integer cantidadDisponible = 0;

    @Column(length = 100)
    private String ubicacion; // Ej: "Almacén A - Estante 3"

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Constructor personalizado
    public Inventario(Producto producto, Integer cantidadDisponible, String ubicacion) {
        this.producto = producto;
        this.cantidadDisponible = cantidadDisponible;
        this.ubicacion = ubicacion;
    }

    /**
     * Verificar si hay stock disponible
     */
    public boolean tieneStock(Integer cantidad) {
        return this.cantidadDisponible >= cantidad;
    }

    /**
     * Verificar si está por debajo del stock mínimo
     */
    public boolean estaBajoStockMinimo() {
        return this.cantidadDisponible <= this.producto.getStockMinimo();
    }

    /**
     * Reducir stock
     */
    public void reducirStock(Integer cantidad) {
        if (cantidad > this.cantidadDisponible) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + this.cantidadDisponible);
        }
        this.cantidadDisponible -= cantidad;
    }

    /**
     * Aumentar stock
     */
    public void aumentarStock(Integer cantidad) {
        this.cantidadDisponible += cantidad;
    }
}