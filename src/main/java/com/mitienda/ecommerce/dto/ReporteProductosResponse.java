package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para reporte de productos más vendidos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteProductosResponse {

    private Integer limite;
    private List<ProductoReporteDTO> productos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoReporteDTO {
        private Long idProducto;
        private String nombreProducto;
        private String skuProducto;
        private String categoria;
        private Long cantidadVendida;
        private BigDecimal montoTotal;
        private Integer stockActual;
    }
}