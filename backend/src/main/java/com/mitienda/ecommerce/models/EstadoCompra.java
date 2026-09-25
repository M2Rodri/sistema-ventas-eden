package com.mitienda.ecommerce.models;

/**
 * Enum para estados de compras
 */
public enum EstadoCompra {
    POR_CONFIRMAR,  // Compra cargada, todavía se puede editar o cancelar
    CONFIRMADA,     // Ya se aplicó al stock y al costo del producto
    CANCELADA       // Compra cancelada
}