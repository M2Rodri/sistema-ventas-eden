package com.mitienda.ecommerce.models;

/**
 * Estados de una compra. Una compra es algo ya comprado: no hay estado previo ni edición;
 * se registra o se anula.
 */
public enum EstadoCompra {
    CONFIRMADA,     // Compra registrada: ya entró al inventario y actualizó el costo del producto
    CANCELADA       // Compra anulada: la mercadería se descontó del inventario
}