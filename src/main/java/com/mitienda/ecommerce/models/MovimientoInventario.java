package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad MovimientoInventario - Auditoría de cambios de stock
 */
@Entity
@Table(name = "movimientos_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_anterior")
    private Integer cantidadAnterior;

    @Column(name = "cantidad_nueva")
    private Integer cantidadNueva;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private String tipoMovimiento;

    @Column(name = "motivo", length = 200)
    private String motivo;

    /** Nota libre del movimiento. La columna existía y la entidad no la mapeaba. */
    @Column(columnDefinition = "text")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    public MovimientoInventario(Producto producto, Integer cantidadAnterior, Integer cantidadNueva,
                       String tipoMovimiento, String motivo, Usuario usuario) {
    this.producto = producto;
    this.cantidadAnterior = cantidadAnterior;
    this.cantidadNueva = cantidadNueva;
    this.cantidad = cantidadNueva - cantidadAnterior;
    this.tipoMovimiento = tipoMovimiento;
    this.motivo = motivo;
    this.usuario = usuario;
}
}