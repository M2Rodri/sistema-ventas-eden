package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.MetodoPago;
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
    private String celularCliente;
    private LocalDateTime fechaVenta;
    private BigDecimal montoTotal;
    private EstadoVenta estado;
    private MetodoPago metodoPago;
    private Long idUsuario;
    private String nombreUsuario;
    private List<DetalleVentaDTO> detalles;
    private List<PagoDTO> pagos;
    private LocalDateTime fechaActualizacion;
    private boolean esClienteRegistrado;

    public VentaResponse(Venta venta) {
        this.id = venta.getId();

        if (venta.getNombreClienteDirecto() != null && !venta.getNombreClienteDirecto().isEmpty()) {
            this.idCliente = null;
            this.nombreCliente = venta.getNombreClienteDirecto();
            this.celularCliente = venta.getCelularClienteDirecto();
            this.esClienteRegistrado = false;
        } else if (venta.getCliente() != null) {
            this.idCliente = venta.getCliente().getId();
            this.nombreCliente = venta.getCliente().getNombreCompleto();
            this.celularCliente = venta.getCliente().getCelular();
            this.esClienteRegistrado = true;
        } else {
            this.idCliente = null;
            this.nombreCliente = "Cliente no especificado";
            this.celularCliente = null;
            this.esClienteRegistrado = false;
        }

        this.fechaVenta = venta.getFechaVenta();
        this.montoTotal = venta.getMontoTotal();
        this.estado = venta.getEstado();
        this.metodoPago = venta.getMetodoPago();
        this.idUsuario = venta.getUsuario() != null ? venta.getUsuario().getId() : null;
        this.nombreUsuario = venta.getUsuario() != null ? venta.getUsuario().getNombreCompleto() : null;
        this.detalles = venta.getDetalles().stream()
                .map(DetalleVentaDTO::new)
                .collect(Collectors.toList());
        this.pagos = venta.getPagos().stream()
                .map(PagoDTO::new)
                .collect(Collectors.toList());
        this.fechaActualizacion = venta.getFechaActualizacion();
    }
}