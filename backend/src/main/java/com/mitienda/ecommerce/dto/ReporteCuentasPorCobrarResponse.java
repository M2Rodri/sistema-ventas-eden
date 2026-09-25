package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para reporte de cuentas por cobrar (ventas con saldo pendiente)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCuentasPorCobrarResponse {

    private Long cantidadVentasPendientes;
    private BigDecimal totalPorCobrar;
    private List<VentaPendienteDTO> ventas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaPendienteDTO {
        private Long idVenta;
        private LocalDateTime fechaVenta;
        private String nombreCliente;
        private String telefonoCliente;
        private BigDecimal montoTotal;
        private BigDecimal saldoPendiente;
        private Long diasTranscurridos;
    }
}
