package com.mitienda.ecommerce.models;

/**
 * Enum para el estado de entrega de una venta.
 *
 * Solo dos estados. ENTREGADO significa que el cliente ya recibió el
 * producto, en cualquier modalidad (también en TRANSPORTADORA):
 *   RETIRO:                       nace ENTREGADO.
 *   DOMICILIO y TRANSPORTADORA:   PENDIENTE <-> ENTREGADO (un ADMIN puede
 *                                 corregir un ENTREGADO a PENDIENTE).
 */
public enum EstadoEntrega {
    PENDIENTE,
    ENTREGADO
}
