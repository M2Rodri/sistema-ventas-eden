package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
 * Entidad Compra - Representa las compras a proveedores
 */
@Entity
@Table(name = "compras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El proveedor es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCompra;

    /**
     * Número de factura del proveedor. Es el respaldo legal del gasto: sin él
     * la compra no es auditable. La base impide cargar dos veces la misma
     * factura del mismo proveedor.
     */
    @Column(name = "numero_factura", length = 50)
    private String numeroFactura;

    /** Suma de los subtotales de las líneas, antes del descuento. */
    @NotNull(message = "El subtotal es obligatorio")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    /**
     * Antes se llamaba costoTotal. Se renombró a montoTotal por simetría con
     * ventas.monto_total: las dos mitades del negocio se leen igual.
     *
     * No lleva @DecimalMin: la compra se crea con importes en cero y recién
     * al agregar las líneas se recalcula el total. Validar "mayor a 0" acá
     * impedía crearla. El importe positivo lo garantizan el CHECK de la base
     * y la validación de cada línea (cantidad > 0, precio >= 0).
     */
    @NotNull(message = "El monto total es obligatorio")
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado = EstadoCompra.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario; // Usuario que registró la compra

    @Column(length = 500)
    private String notas;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relación con detalle de compra
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleCompra> detalles = new ArrayList<>();

    // Constructor personalizado
    public Compra(Proveedor proveedor, BigDecimal montoTotal, Usuario usuario) {
        this.proveedor = proveedor;
        this.subtotal = montoTotal;
        this.descuento = BigDecimal.ZERO;
        this.montoTotal = montoTotal;
        this.usuario = usuario;
        this.estado = EstadoCompra.PENDIENTE;
    }

    /** Recalcula subtotal y total a partir de las líneas de la compra. */
    public void calcularTotal() {
        this.subtotal = detalles.stream()
                .map(DetalleCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desc = this.descuento != null ? this.descuento : BigDecimal.ZERO;
        this.montoTotal = this.subtotal.subtract(desc);
    }
}