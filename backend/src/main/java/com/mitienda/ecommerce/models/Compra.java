package com.mitienda.ecommerce.models;

import com.mitienda.ecommerce.config.GeneradorIdSinHuecos;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
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

/** Entidad Compra - Representa las compras a proveedores */
@Entity
@Table(name = "compras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Compra {
    @Id
    @GeneratedValue(generator = "id_compras")
    @GenericGenerator(name = "id_compras", type = GeneradorIdSinHuecos.class,
            parameters = @Parameter(name = "tabla", value = "compras"))
    private Long id;

    /** Puede faltar: a veces se compra sin un proveedor formal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCompra;

    /** Número de factura del proveedor; la base impide repetirlo para el mismo proveedor. */
    @Column(name = "numero_factura", length = 50)
    private String numeroFactura;

    /** Suma de los subtotales de las líneas, antes del descuento. */
    @NotNull(message = "El subtotal es obligatorio")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    /** Sin @DecimalMin: la compra nace con importes en cero y el total se recalcula al agregar líneas. */
    @NotNull(message = "El monto total es obligatorio")
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado = EstadoCompra.CONFIRMADA;

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
        this.estado = EstadoCompra.CONFIRMADA;
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