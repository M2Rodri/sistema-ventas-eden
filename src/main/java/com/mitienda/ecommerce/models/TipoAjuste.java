package com.mitienda.ecommerce.models;

/**
 * Enum para tipos de ajuste de inventario
 */
public enum TipoAjuste {
    ENTRADA,        // Aumento de stock manual
    SALIDA,         // Disminución de stock manual
    COMPRA,         // Entrada por compra a proveedor
    VENTA,          // Salida por venta
    DEVOLUCION,     // Devolución de cliente
    MERMA,          // Pérdida por daño/vencimiento
    AJUSTE_INICIAL  // Ajuste inicial de inventario
}