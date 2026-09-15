package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para estadísticas generales de clientes
 * Usado en Interfaz P6.1 - Indicadores superiores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEstadisticas {
    
    // Total de clientes registrados
    private Long totalClientes;
    
    // Clientes con compras este mes
    private Long clientesConComprasEsteMes;
    
    // Cliente con mayor monto de compras
    private String clienteTopNombre;
    private BigDecimal clienteTopMonto;
}