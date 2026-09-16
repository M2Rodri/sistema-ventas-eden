package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Pago - Registro de pagos de ventas
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La venta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaPago;

    @Column(length = 100)
    private String referencia; // Número de referencia del pago

    /**
     * Nota libre del cobro. La columna existía en la base y la entidad no la
     * mapeaba: sirve para anotar el contexto de un pago (a cuenta, cheque
     * diferido, quién lo entregó).
     */
    @Column(columnDefinition = "text")
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.COMPLETADO;

    /**
     * Foto del comprobante (QR/transferencia). Opcional siempre: si falta en
     * un pago que no es EFECTIVO, el pago queda "sin respaldo" (ver
     * PagoDTO.sinRespaldo), pero nunca bloquea el registro. Se puede
     * adjuntar en el momento o después, sobre un pago ya guardado.
     */
    @Column(length = 500)
    private String urlComprobante;

    // Constructor personalizado
    public Pago(Venta venta, BigDecimal monto, MetodoPago metodoPago, String referencia) {
        this.venta = venta;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.estado = EstadoPago.COMPLETADO;
    }
}