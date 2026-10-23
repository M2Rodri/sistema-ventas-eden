package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTO para reporte de ventas */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasResponse {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long totalVentas;
    private BigDecimal montoTotalVentas;
    private BigDecimal ticketPromedio;
    private BigDecimal gananciaVentas;
    private List<VentaDetalleDTO> ventas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaDetalleDTO {
        private Long idVenta;
        private LocalDateTime fechaVenta;
        private String nombreCliente;
        private BigDecimal montoTotal;
        private String metodoPago;
        private String estado;
        /** Costo total de lo vendido en esta venta (suma de costo_unitario x cantidad); null si alguna línea no tiene costo. */
        private BigDecimal costoTotal;
        /** Ganancia de esta venta; null si no tiene costo o es cancelada. */
        private BigDecimal ganancia;
    }
}