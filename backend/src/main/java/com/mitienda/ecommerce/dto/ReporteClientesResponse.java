package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para reporte de clientes frecuentes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteClientesResponse {

    private Integer limite;
    private List<ClienteReporteDTO> clientes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteReporteDTO {
        private Long idCliente;
        private String nombreCliente;
        private String telefono;
        private String email;
        private Long cantidadCompras;
        private BigDecimal montoTotalCompras;
        private BigDecimal promedioCompra;
    }
}
