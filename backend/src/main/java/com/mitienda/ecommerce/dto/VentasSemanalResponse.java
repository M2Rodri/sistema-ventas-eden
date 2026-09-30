package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ventas de una semana calendario (lunes a domingo), con el desglose por día.
 *
 * Cuenta solo ventas COMPLETADA, igual que ventasStats del dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentasSemanalResponse {

    /** Lunes de la semana, formato "2026-09-28". */
    private String fechaInicio;

    /** Domingo de la semana, formato "2026-10-04". */
    private String fechaFin;

    /** Número de semana ISO del año (1 a 53). */
    private Integer numeroSemana;

    /** true si la semana pedida es la que está en curso. */
    private Boolean esSemanaActual;

    private Long totalVentas;
    private BigDecimal montoTotal;

    /** Siempre 7 elementos, de lunes a domingo; los días sin ventas van en 0. */
    private List<DashboardResponse.VentaPorDiaDTO> ventasPorDia;
}
