package com.mitienda.ecommerce.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
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

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @Column(name = "nombre_cliente_directo", length = 100)
    private String nombreClienteDirecto;

    @Column(name = "celular_cliente_directo", length = 20)
    private String celularClienteDirecto;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaVenta;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto total debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE_PAGO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private User usuario;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    public Venta(Cliente cliente, BigDecimal montoTotal, MetodoPago metodoPago, User usuario) {
        this.cliente = cliente;
        this.montoTotal = montoTotal;
        this.metodoPago = metodoPago;
        this.usuario = usuario;
        this.estado = EstadoVenta.PENDIENTE_PAGO;
    }

    public Venta(String nombreClienteDirecto, String celularClienteDirecto, BigDecimal montoTotal, MetodoPago metodoPago, User usuario) {
        this.nombreClienteDirecto = nombreClienteDirecto;
        this.celularClienteDirecto = celularClienteDirecto;
        this.montoTotal = montoTotal;
        this.metodoPago = metodoPago;
        this.usuario = usuario;
        this.estado = EstadoVenta.PENDIENTE_PAGO;
    }

    public String getNombreClienteCompleto() {
        if (nombreClienteDirecto != null && !nombreClienteDirecto.isEmpty()) {
            return nombreClienteDirecto;
        }
        return cliente != null ? cliente.getNombreCompleto() : "Cliente no especificado";
    }

    public String getCelularClienteCompleto() {
        if (celularClienteDirecto != null && !celularClienteDirecto.isEmpty()) {
            return celularClienteDirecto;
        }
        return cliente != null ? cliente.getCelular() : null;
    }
}