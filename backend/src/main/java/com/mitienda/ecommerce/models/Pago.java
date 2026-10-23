package com.mitienda.ecommerce.models;

import com.mitienda.ecommerce.config.GeneradorIdSinHuecos;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entidad Pago - Registro de pagos de ventas */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    @Id
    @GeneratedValue(generator = "id_pagos")
    @GenericGenerator(name = "id_pagos", type = GeneradorIdSinHuecos.class,
            parameters = @Parameter(name = "tabla", value = "pagos"))
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

    /** Nota libre del cobro (a cuenta, cheque diferido, quién lo entregó). */
    @Column(columnDefinition = "text")
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.COMPLETADO;

    /** Foto del comprobante; opcional, y un pago no EFECTIVO sin ella queda "sin respaldo" (ver PagoDTO.sinRespaldo). */
    @Column(length = 500)
    private String urlComprobante;

    /** Quién registró el cobro; nulo en pagos antiguos, en los nuevos sale del token. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    // Constructor personalizado
    public Pago(Venta venta, BigDecimal monto, MetodoPago metodoPago, String referencia) {
        this.venta = venta;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.estado = EstadoPago.COMPLETADO;
    }
}