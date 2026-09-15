package com.mitienda.ecommerce.models;

/**
 * Enum para estados de compras
 */
public enum EstadoCompra {
    PENDIENTE,      // Orden creada
    CONFIRMADA,     // Proveedor confirmó
    EN_TRANSITO,    // Productos en camino
    RECIBIDA,       // Productos recibidos
    CANCELADA       // Compra cancelada
}