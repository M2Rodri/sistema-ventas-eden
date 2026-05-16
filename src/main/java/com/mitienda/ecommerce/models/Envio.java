package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "envios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @NotBlank(message = "La dirección de destino es obligatoria")
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    @Column(nullable = false, length = 300)
    private String direccionDestino;

    @Column(length = 50)
    private String ciudad;

    @Column(length = 50)
    private String departamento;

    @NotNull(message = "La fecha de entrega estimada es obligatoria")
    @Column(nullable = false)
    private LocalDate fechaEntregaEstimada;

    @Column
    private LocalDate fechaEntregaReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estadoSeguimiento = EstadoEnvio.PENDIENTE;

    @Column(length = 100)
    private String guiaRemision;

    @DecimalMin(value = "0.0", message = "El costo de envío no puede ser negativo")
    @Column(precision = 10, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transportadora")
    private Transportadora transportadora;

    @Column(length = 500)
    private String notas;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    public Envio(String direccionDestino, LocalDate fechaEntregaEstimada, BigDecimal costoEnvio) {
        this.direccionDestino = direccionDestino;
        this.fechaEntregaEstimada = fechaEntregaEstimada;
        this.costoEnvio = costoEnvio;
        this.estadoSeguimiento = EstadoEnvio.PENDIENTE;
    }
}