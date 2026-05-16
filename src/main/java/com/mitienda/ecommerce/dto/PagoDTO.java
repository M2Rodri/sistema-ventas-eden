package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.EstadoPago;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.Pago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para pagos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {

    private Long id;
    private Long idVenta;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private LocalDateTime fechaPago;
    private String referencia;
    private EstadoPago estado;

    // Constructor desde entidad
    public PagoDTO(Pago pago) {
        this.id = pago.getId();
        this.idVenta = pago.getVenta().getId();
        this.monto = pago.getMonto();
        this.metodoPago = pago.getMetodoPago();
        this.fechaPago = pago.getFechaPago();
        this.referencia = pago.getReferencia();
        this.estado = pago.getEstado();
    }
}