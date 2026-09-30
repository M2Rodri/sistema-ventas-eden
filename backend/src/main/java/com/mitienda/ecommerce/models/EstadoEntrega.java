package com.mitienda.ecommerce.models;

/**
 * Enum para el estado de entrega de una venta.
 *
 * El recorrido depende de la modalidad:
 *   RETIRO:         queda ENTREGADO al registrar la venta.
 *   DOMICILIO:      PENDIENTE -> ENTREGADO.
 *   TRANSPORTADORA: PENDIENTE -> DESPACHADO -> ENTREGADO (también se puede
 *                   pasar de PENDIENTE a ENTREGADO directamente).
 */
public enum EstadoEntrega {
    PENDIENTE,
    DESPACHADO,
    ENTREGADO
}
