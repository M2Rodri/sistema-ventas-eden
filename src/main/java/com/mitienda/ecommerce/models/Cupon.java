package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Cupon - Cupones de descuento
 */
@Entity
@Table(name = "cupones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 3, max = 50, message = "El código debe tener entre 3 y 50 caracteres")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo; // Ej: "VERANO2025", "PRIMERACOMPRA"

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    @Column(nullable = false, length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCupon tipoCupon;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @Column(precision = 10, scale = 2)
    private BigDecimal valorDescuento; // Porcentaje o monto fijo

    @DecimalMin(value = "0.0", message = "El monto mínimo no puede ser negativo")
    @Column(precision = 10, scale = 2)
    private BigDecimal montoMinimo = BigDecimal.ZERO; // Compra mínima para aplicar

    @DecimalMin(value = "0.0", message = "El descuento máximo no puede ser negativo")
    @Column(precision = 10, scale = 2)
    private BigDecimal descuentoMaximo; // Límite de descuento para porcentajes

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Min(value = 1, message = "El uso máximo debe ser al menos 1")
    @Column
    private Integer usoMaximo; // null = ilimitado

    @Column(nullable = false)
    private Integer vecesUsado = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean primeraCompra = false; // Solo para primera compra del cliente

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Constructor personalizado
    public Cupon(String codigo, String descripcion, TipoCupon tipoCupon, BigDecimal valorDescuento,
                 LocalDate fechaInicio, LocalDate fechaFin) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tipoCupon = tipoCupon;
        this.valorDescuento = valorDescuento;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = true;
        this.vecesUsado = 0;
    }

    /**
     * Verificar si el cupón está vigente
     */
    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        return this.activo && 
               !hoy.isBefore(this.fechaInicio) && 
               !hoy.isAfter(this.fechaFin) &&
               (this.usoMaximo == null || this.vecesUsado < this.usoMaximo);
    }

    /**
     * Calcular descuento para un monto específico
     */
    public BigDecimal calcularDescuento(BigDecimal montoCompra) {
        if (!estaVigente() || montoCompra.compareTo(this.montoMinimo) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal descuento = BigDecimal.ZERO;

        switch (this.tipoCupon) {
            case PORCENTAJE:
                descuento = montoCompra.multiply(this.valorDescuento.divide(BigDecimal.valueOf(100)));
                if (this.descuentoMaximo != null && descuento.compareTo(this.descuentoMaximo) > 0) {
                    descuento = this.descuentoMaximo;
                }
                break;
            case MONTO_FIJO:
                descuento = this.valorDescuento;
                if (descuento.compareTo(montoCompra) > 0) {
                    descuento = montoCompra;
                }
                break;
            case ENVIO_GRATIS:
                descuento = BigDecimal.ZERO; // Manejado aparte
                break;
        }

        return descuento;
    }

    /**
     * Incrementar contador de uso
     */
    public void incrementarUso() {
        this.vecesUsado++;
    }
}