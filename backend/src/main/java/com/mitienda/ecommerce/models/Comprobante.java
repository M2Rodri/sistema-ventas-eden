package com.mitienda.ecommerce.models;

import com.mitienda.ecommerce.config.GeneradorIdSinHuecos;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Comprobante - Recibos simples de venta
 */
@Entity
@Table(name = "comprobantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(generator = "id_comprobantes")
    @GenericGenerator(name = "id_comprobantes", type = GeneradorIdSinHuecos.class,
            parameters = @Parameter(name = "tabla", value = "comprobantes"))
    private Long id;

    @NotNull(message = "La venta es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @NotBlank(message = "El número de comprobante es obligatorio")
    @Column(nullable = false, unique = true, length = 50)
    private String numeroComprobante; // Ej: REC-2025-00001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComprobante tipoComprobante = TipoComprobante.RECIBO;

    @NotNull(message = "El monto total es obligatorio")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(length = 200)
    private String nombreCliente; // Opcional, nombre del cliente

    @Column(length = 500)
    private String observaciones;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false)
    private Boolean anulado = false;

    // Constructor personalizado
    public Comprobante(Venta venta, String numeroComprobante, TipoComprobante tipoComprobante) {
        this.venta = venta;
        this.numeroComprobante = numeroComprobante;
        this.tipoComprobante = tipoComprobante;
        this.montoTotal = venta.getMontoTotal();
        this.anulado = false;
    }
}