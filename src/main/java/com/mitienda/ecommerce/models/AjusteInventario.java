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
 * Entidad AjusteInventario - Auditoría de cambios de stock
 */
@Entity
@Table(name = "movimientos_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjusteInventario {

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

    @Column(name = "cantidad", nullable = false)
    private Integer diferencia;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipoAjuste;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private User usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    public AjusteInventario(Producto producto, Integer cantidadAnterior, Integer cantidadNueva,
                       String tipoAjuste, String motivo, User usuario) {
    this.producto = producto;
    this.cantidadAnterior = cantidadAnterior;
    this.cantidadNueva = cantidadNueva;
    this.diferencia = cantidadNueva - cantidadAnterior;
    this.tipoAjuste = tipoAjuste;
    this.motivo = motivo;
    this.usuario = usuario;
}
}