package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.EstadoEntrega;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.ModalidadEntrega;
import com.mitienda.ecommerce.models.Venta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private String ciCliente;
    private LocalDateTime fechaVenta;

    // Importes: los cuatro que guarda la tabla, no solo el total
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;
    private LocalDate fechaLimitePago;

    /** Calculado con el reloj del servidor: hay pago pendiente y su fecha límite ya pasó. */
    private Boolean fechaLimiteVencida;

    private EstadoVenta estado;

    private ModalidadEntrega modalidadEntrega;
    private EstadoEntrega estadoEntrega;
    private String direccionDestino;
    private String ciudad;
    private String transportadora;
    private String guiaRemision;

    /** Método de pago del listado: el único, "VARIOS" si hay más de uno, o null si no se cobró nada. */
    private String metodoPago;

    private Long idUsuario;
    private String nombreUsuario;
    private List<DetalleVentaDTO> detalles;
    private List<PagoDTO> pagos;
    private LocalDateTime fechaActualizacion;

    /** Al menos uno de los pagos de la venta es QR/transferencia sin foto de comprobante. */
    private boolean tienePagosSinRespaldo;

    /** Costo total de lo vendido en la venta (solo disponible para ADMIN); null si no cuenta o empleado. */
    private BigDecimal costoTotal;
    /** Ganancia total estimada de la venta (montoTotal - costoTotal) (solo ADMIN); null si no cuenta o empleado. */
    private BigDecimal ganancia;

    /** Respuesta sin costos ni ganancia (lo que ve un Empleado); los costos se piden con el otro constructor. */
    public VentaResponse(Venta venta) {
        this(venta, false);
    }

    /** @param incluirCostos true solo para el Admin: costoTotal, ganancia y el costo unitario de cada línea. */
    public VentaResponse(Venta venta, boolean incluirCostos) {
        this.id = venta.getId();

        if (venta.getCliente() != null) {
            this.idCliente = venta.getCliente().getId();
            this.nombreCliente = venta.getCliente().getNombreCompleto();
            this.telefonoCliente = venta.getCliente().getTelefono();
            this.ciCliente = venta.getCliente().getNitCi();
        } else {
            this.idCliente = null;
            this.nombreCliente = "Cliente no especificado";
            this.telefonoCliente = null;
            this.ciCliente = null;
        }

        this.fechaVenta = venta.getFechaVenta();
        this.subtotal = venta.getSubtotal();
        this.descuento = venta.getDescuento();
        this.montoTotal = venta.getMontoTotal();
        this.saldoPendiente = venta.getSaldoPendiente();
        this.fechaLimitePago = venta.getFechaLimitePago();
        this.fechaLimiteVencida = venta.getFechaLimitePago() != null
                && venta.getEstado() == EstadoVenta.PENDIENTE_PAGO
                && venta.getSaldoPendiente() != null
                && venta.getSaldoPendiente().compareTo(java.math.BigDecimal.ZERO) > 0
                && venta.getFechaLimitePago().isBefore(LocalDate.now());
        this.estado = venta.getEstado();

        this.modalidadEntrega = venta.getModalidadEntrega();
        this.estadoEntrega = venta.getEstadoEntrega();
        this.direccionDestino = venta.getDireccionDestino();
        this.ciudad = venta.getCiudad();
        this.transportadora = venta.getTransportadora();
        this.guiaRemision = venta.getGuiaRemision();

        this.idUsuario = venta.getUsuario() != null ? venta.getUsuario().getId() : null;
        this.nombreUsuario = venta.getUsuario() != null ? venta.getUsuario().getNombreCompleto() : null;

        this.detalles = venta.getDetalles().stream()
                .map(detalle -> new DetalleVentaDTO(detalle, incluirCostos))
                .collect(Collectors.toList());
        this.pagos = venta.getPagos().stream()
                .map(PagoDTO::new)
                .collect(Collectors.toList());
        this.tienePagosSinRespaldo = this.pagos.stream().anyMatch(PagoDTO::isSinRespaldo);

        this.metodoPago = derivarMetodoPago(venta);
        this.fechaActualizacion = venta.getFechaActualizacion();

        // Rentabilidad: solo para el Admin, y si la venta tiene detalles y no está cancelada
        if (incluirCostos && venta.getEstado() != EstadoVenta.CANCELADA && venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            boolean tieneCostosCompletos = true;
            BigDecimal costoAcumulado = BigDecimal.ZERO;
            for (com.mitienda.ecommerce.models.DetalleVenta d : venta.getDetalles()) {
                if (d.getCostoUnitario() == null) {
                    tieneCostosCompletos = false;
                    break;
                }
                costoAcumulado = costoAcumulado.add(d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
            }
            if (tieneCostosCompletos) {
                this.costoTotal = costoAcumulado;
                this.ganancia = venta.getMontoTotal().subtract(costoAcumulado);
            }
        }
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
