package com.mitienda.ecommerce.models;

/**
 * Enum para tipos de movimiento de inventario
 */
public enum TipoMovimiento {
    ENTRADA,        // Aumento de stock manual
    SALIDA,         // Disminución de stock manual
    COMPRA,         // Entrada por compra a proveedor
    VENTA,          // Salida por venta
    DEVOLUCION,     // Devolución de cliente
    MERMA,          // Pérdida por daño/vencimiento
    AJUSTE_INICIAL  // Movimiento inicial de inventario
}