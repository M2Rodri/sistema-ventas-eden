package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Comprobante;
import com.mitienda.ecommerce.models.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respuestas de comprobante
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteResponse {

    private Long id;
    private Long idVenta;
    private String numeroComprobante;
    private TipoComprobante tipoComprobante;
    private BigDecimal montoTotal;
    private String nombreCliente;
    private String observaciones;
    private LocalDateTime fechaEmision;
    private Boolean anulado;
    private LocalDateTime fechaAnulacion;
    private String motivoAnulacion;

    // Constructor desde entidad
    public ComprobanteResponse(Comprobante comprobante) {
        this.id = comprobante.getId();
        this.idVenta = comprobante.getVenta().getId();
        this.numeroComprobante = comprobante.getNumeroComprobante();
        this.tipoComprobante = comprobante.getTipoComprobante();
        this.montoTotal = comprobante.getMontoTotal();
        this.nombreCliente = comprobante.getNombreCliente();
        this.observaciones = comprobante.getObservaciones();
        this.fechaEmision = comprobante.getFechaEmision();
        this.anulado = comprobante.getAnulado();
        this.fechaAnulacion = comprobante.getFechaAnulacion();
        this.motivoAnulacion = comprobante.getMotivoAnulacion();
    }
}