package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuesta de historial de compras de un cliente
 * Usado en CU: Ver Historial de Compras del Cliente - Interfaz P6.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialComprasResponse {

    // Información del cliente
    private ClienteResponse cliente;

    // Estadísticas de compra
    private EstadisticasCompra estadisticas;

    // Historial de ventas
    private List<VentaResponse> ventas;

    /**
     * Clase interna para estadísticas de compra
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasCompra {
        
        // Total de compras realizadas (cantidad)
        private Long totalCompras;
        
        // Monto total gastado en bolivianos
        private BigDecimal montoTotal;
        
        // Ticket promedio de compra
        private BigDecimal ticketPromedio;
        
        // Última fecha de compra
        private LocalDateTime ultimaCompra;
        
        // Producto más comprado (nombre)
        private String productoMasComprado;
        
        // Cantidad del producto más comprado
        private Integer cantidadProductoMasComprado;
    }
}