package com.mitienda.ecommerce.models;

import com.mitienda.ecommerce.config.GeneradorIdSinHuecos;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
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
    @GeneratedValue(generator = "id_inventario")
    @GenericGenerator(name = "id_inventario", type = GeneradorIdSinHuecos.class,
            parameters = @Parameter(name = "tabla", value = "inventario"))
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false, unique = true)
    private Producto producto;

    @NotNull(message = "La cantidad disponible es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    private Integer cantidadDisponible = 0;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Constructor personalizado
    public Inventario(Producto producto, Integer cantidadDisponible) {
        this.producto = producto;
        this.cantidadDisponible = cantidadDisponible;
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
        // Un mínimo de 0 es "sin alerta": producto bajo pedido o que ya no se compra.
        return this.producto.getStockMinimo() > 0
                && this.cantidadDisponible <= this.producto.getStockMinimo();
    }

    /**
     * Reducir stock
     */
    public void reducirStock(Integer cantidad) {
        if (cantidad > this.cantidadDisponible) {
            throw new ReglaNegocioException("STOCK_INSUFICIENTE", "Stock insuficiente. Disponible: " + this.cantidadDisponible);
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