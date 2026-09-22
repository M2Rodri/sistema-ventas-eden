package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para el reporte financiero.
 *
 * Separa dos cosas que antes se mezclaban en un solo numero:
 * - gananciaVentas: la ganancia real de lo vendido (precio menos costo de
 *   cada producto, por la cantidad). Sale de detalle_venta.costo_unitario,
 *   el costo que quedo fijo al momento de cada venta.
 * - ingresosTotales / gastosTotales / saldoPeriodo: flujo de caja del
 *   periodo (ventas contra compras), que no es ganancia. Un mes en que el
 *   negocio compra para abastecerse puede dar saldo negativo aunque haya
 *   vendido con margen positivo.
 */
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
