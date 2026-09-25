package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.EstadoEntrega;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.ModalidadEntrega;
import com.mitienda.ecommerce.models.Venta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {

    private Long id;
    private Long idCliente;
    private String nombreCliente;
    private String telefonoCliente;
    private LocalDateTime fechaVenta;

    // Importes: los cuatro que guarda la tabla, no solo el total
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;

    private EstadoVenta estado;
    private Boolean requiereEnvio;

    private ModalidadEntrega modalidadEntrega;
    private EstadoEntrega estadoEntrega;
    private String direccionDestino;
    private String ciudad;
    private String transportadora;
    private String guiaRemision;

    /**
     * Método de pago mostrado en los listados.
     *
     * Ya no es un campo de la tabla 'ventas': se deriva de los pagos, porque
     * una venta admite varios cobros con métodos distintos. Vale el método
     * cuando hay uno solo, "VARIOS" cuando hay más de uno, y null si todavía
     * no se cobró nada. El detalle real está en la lista de pagos.
     */
    private String metodoPago;

    private Long idUsuario;
    private String nombreUsuario;
    private List<DetalleVentaDTO> detalles;
    private List<PagoDTO> pagos;
    private LocalDateTime fechaActualizacion;

    /** Al menos uno de los pagos de la venta es QR/transferencia sin foto de comprobante. */
    private boolean tienePagosSinRespaldo;

    public VentaResponse(Venta venta) {
        this.id = venta.getId();

        if (venta.getCliente() != null) {
            this.idCliente = venta.getCliente().getId();
            this.nombreCliente = venta.getCliente().getNombreCompleto();
            this.telefonoCliente = venta.getCliente().getTelefono();
        } else {
            this.idCliente = null;
            this.nombreCliente = "Cliente no especificado";
            this.telefonoCliente = null;
        }

        this.fechaVenta = venta.getFechaVenta();
        this.subtotal = venta.getSubtotal();
        this.descuento = venta.getDescuento();
        this.montoTotal = venta.getMontoTotal();
        this.saldoPendiente = venta.getSaldoPendiente();
        this.estado = venta.getEstado();
        this.requiereEnvio = venta.getRequiereEnvio();

        this.modalidadEntrega = venta.getModalidadEntrega();
        this.estadoEntrega = venta.getEstadoEntrega();
        this.direccionDestino = venta.getDireccionDestino();
        this.ciudad = venta.getCiudad();
        this.transportadora = venta.getTransportadora();
        this.guiaRemision = venta.getGuiaRemision();

        this.idUsuario = venta.getUsuario() != null ? venta.getUsuario().getId() : null;
        this.nombreUsuario = venta.getUsuario() != null ? venta.getUsuario().getNombreCompleto() : null;

        this.detalles = venta.getDetalles().stream()
                .map(DetalleVentaDTO::new)
                .collect(Collectors.toList());
        this.pagos = venta.getPagos().stream()
                .map(PagoDTO::new)
                .collect(Collectors.toList());
        this.tienePagosSinRespaldo = this.pagos.stream().anyMatch(PagoDTO::isSinRespaldo);

        this.metodoPago = derivarMetodoPago(venta);
        this.fechaActualizacion = venta.getFechaActualizacion();
    }

    /** Resume en un texto los métodos usados en los pagos de la venta. */
    private String derivarMetodoPago(Venta venta) {
        if (venta.getPagos() == null || venta.getPagos().isEmpty()) {
            return null;
        }
        List<String> metodos = venta.getPagos().stream()
                .filter(p -> p.getMetodoPago() != null)
                .map(p -> p.getMetodoPago().name())
                .distinct()
                .collect(Collectors.toList());

        if (metodos.isEmpty()) {
            return null;
        }
        return metodos.size() == 1 ? metodos.get(0) : "VARIOS";
    }
}
