package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidad DetalleVenta - Productos dentro de una venta
 * Incluye soporte para descuentos y auditoría de precios
 */
@Entity
@Table(name = "detalle_venta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La venta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(nullable = false)
    private Integer cantidad;

    // Precio original del producto (para auditoría)
    @Column(name = "precio_unitario_original", precision = 10, scale = 2)
    private BigDecimal precioUnitarioOriginal;

    // Precio final aplicado (puede tener descuento)
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // Descuento aplicado por unidad (en monto)
    @Column(name = "descuento_unitario", precision = 10, scale = 2)
    private BigDecimal descuentoUnitario = BigDecimal.ZERO;

    // Porcentaje de descuento aplicado
    @Column(name = "descuento_porcentaje", precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El subtotal debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Costo del producto en el momento de la venta (copiado de
     * producto.precioCompra al crear el detalle). Queda fijo: si el
     * precio de compra cambia despues, la ganancia de esta venta no se
     * recalcula sola. No se expone en DetalleVentaDTO: el rol EMPLEADO
     * puede ver ventas y no tiene que ver costos ni margenes.
     */
    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo no puede ser negativo")
    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    // Constructor personalizado (sin descuento)
    public DetalleVenta(Venta venta, Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitarioOriginal = precioUnitario;
        this.precioUnitario = precioUnitario;
        this.descuentoUnitario = BigDecimal.ZERO;
        this.descuentoPorcentaje = BigDecimal.ZERO;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Constructor con descuento
     */
    public DetalleVenta(Venta venta, Producto producto, Integer cantidad, 
                       BigDecimal precioOriginal, BigDecimal precioConDescuento) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitarioOriginal = precioOriginal;
        this.precioUnitario = precioConDescuento;
        
        // Calcular descuento
        this.descuentoUnitario = precioOriginal.subtract(precioConDescuento);
        
        // Calcular porcentaje de descuento
        if (precioOriginal.compareTo(BigDecimal.ZERO) > 0) {
            this.descuentoPorcentaje = this.descuentoUnitario
                .divide(precioOriginal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        this.subtotal = precioConDescuento.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Calcular subtotal automáticamente
     */
    public void calcularSubtotal() {
        this.subtotal = this.precioUnitario.multiply(BigDecimal.valueOf(this.cantidad));
    }

    /**
     * Aplicar descuento porcentual
     */
    public void aplicarDescuentoPorcentaje(BigDecimal porcentaje) {
        if (this.precioUnitarioOriginal == null) {
            this.precioUnitarioOriginal = this.precioUnitario;
        }
        
        this.descuentoPorcentaje = porcentaje;
        
        // Calcular precio con descuento
        BigDecimal descuentoDecimal = porcentaje.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        this.descuentoUnitario = this.precioUnitarioOriginal.multiply(descuentoDecimal);
        this.precioUnitario = this.precioUnitarioOriginal.subtract(this.descuentoUnitario);
        
        calcularSubtotal();
    }

    /**
     * Aplicar precio manual (calcula descuento automático)
     */
    public void aplicarPrecioManual(BigDecimal nuevoPrecio) {
        if (this.precioUnitarioOriginal == null) {
            this.precioUnitarioOriginal = this.precioUnitario;
        }
        
        this.precioUnitario = nuevoPrecio;
        this.descuentoUnitario = this.precioUnitarioOriginal.subtract(nuevoPrecio);
        
        // Calcular porcentaje
        if (this.precioUnitarioOriginal.compareTo(BigDecimal.ZERO) > 0) {
            this.descuentoPorcentaje = this.descuentoUnitario
                .divide(this.precioUnitarioOriginal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        calcularSubtotal();
    }

    /**
     * Obtener monto total de descuento (unitario * cantidad)
     */
    public BigDecimal getDescuentoTotal() {
        return this.descuentoUnitario.multiply(BigDecimal.valueOf(this.cantidad));
    }
}