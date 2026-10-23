package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Reporte financiero: ganancia de las ventas y flujo de caja (ingresos, gastos, saldo), que no es ganancia. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteFinancieroResponse {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    private BigDecimal gananciaVentas;

    private BigDecimal ingresosTotales;
    private BigDecimal gastosTotales;
    private BigDecimal saldoPeriodo;

    private List<DetalleFechaMontoDTO> detalleIngresos;
    private List<DetalleFechaMontoDTO> detalleGastos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleFechaMontoDTO {
        private String fecha;
        private BigDecimal monto;
    }
}
