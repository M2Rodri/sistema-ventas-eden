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

/**
 * Entidad Venta - Cabecera de una operación de venta (tabla 'ventas').
 *
 * Cambios respecto del esquema anterior:
 *
 *  - metodoPago se eliminó. Una venta admite varios pagos con métodos
 *    distintos (parte en efectivo, parte por transferencia), así que un único
 *    campo acá no podía representar el caso real. La fuente de verdad es
 *    pagos.metodo_pago.
 *
 *  - nombreClienteDirecto y celularClienteDirecto se eliminaron. Duplicaban
 *    lo que ya modela clientes.tipo_cliente = INVITADO: la venta de mostrador
 *    sin datos del comprador se registra creando un cliente INVITADO.
 *
 *  - subtotal, descuento, saldoPendiente y requiereEnvio se agregaron: existían
 *    en la base pero la entidad no los mapeaba. subtotal es NOT NULL, así que
 *    sin mapearlo todo INSERT de venta fallaba.
 *
 *  - usuario pasa a ser obligatorio: no existe una venta sin vendedor.
 */
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

    /** Vendedor que registró la venta. Obligatorio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaVenta;

    /** Suma de los subtotales de las líneas, antes del descuento general. */
    @NotNull(message = "El subtotal es obligatorio")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** Descuento general aplicado sobre el subtotal. */
    @Column(precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto total debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    /** Lo que falta cobrar: montoTotal menos la suma de los pagos completados. */
    @Column(precision = 10, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE_PAGO;

    /** Si es true, la venta genera un registro en envios. */
    @Column(name = "requiere_envio")
    private Boolean requiereEnvio = false;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    public Venta(Cliente cliente, BigDecimal subtotal, BigDecimal montoTotal, Usuario usuario) {
        this.cliente = cliente;
        this.subtotal = subtotal;
        this.montoTotal = montoTotal;
        this.saldoPendiente = montoTotal;
        this.usuario = usuario;
        this.estado = EstadoVenta.PENDIENTE_PAGO;
    }

    /** Nombre a mostrar del comprador. Los invitados son clientes tipo INVITADO. */
    public String getNombreClienteCompleto() {
        return cliente != null ? cliente.getNombreCompleto() : "Cliente no especificado";
    }

    /** Teléfono del comprador, si está cargado. */
    public String getTelefonoClienteCompleto() {
        return cliente != null ? cliente.getTelefono() : null;
    }
}
