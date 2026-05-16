package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad AlertaInventario - Alertas de stock bajo
 */
@Entity
@Table(name = "alertas_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidadActual;

    @Column(nullable = false)
    private Integer cantidadMinima;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAlerta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlerta estado = EstadoAlerta.PENDIENTE;

    // Constructor personalizado
    public AlertaInventario(Producto producto, Integer cantidadActual, Integer cantidadMinima) {
        this.producto = producto;
        this.cantidadActual = cantidadActual;
        this.cantidadMinima = cantidadMinima;
        this.estado = EstadoAlerta.PENDIENTE;
    }
}