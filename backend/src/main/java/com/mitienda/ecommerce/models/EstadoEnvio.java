package com.mitienda.ecommerce.models;

/**
 * Enum para estados de envíos
 */
public enum EstadoEnvio {
    PENDIENTE,          // Sin asignar transportadora
    EN_PREPARACION,     // Preparando paquete
    EN_CAMINO,          // En ruta de entrega
    ENTREGADO,          // Entregado al cliente
    DEVUELTO,           // Devuelto al almacén
    CANCELADO           // Envío cancelado
}