package com.mitienda.ecommerce.models;

/** Estados de una compra, que se registra o se anula (no hay estado previo ni edición). */
public enum EstadoCompra {
    CONFIRMADA,     // Compra registrada: ya entró al inventario y actualizó el costo del producto
    CANCELADA       // Compra anulada: la mercadería se descontó del inventario
}