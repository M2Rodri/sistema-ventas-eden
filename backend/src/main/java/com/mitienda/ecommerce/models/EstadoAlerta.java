package com.mitienda.ecommerce.models;

/** Enum para estados de alertas de inventario */
public enum EstadoAlerta {
    /** Hay que mirarla: el producto está bajo el mínimo. */
    PENDIENTE,
    /** Se resolvió sola: el producto se repuso por encima del mínimo. */
    ATENDIDA,
    /** Atendida por el dueño sin reponer: no avisa de nuevo mientras siga bajo el mínimo. */
    ATENDIDA_MANUAL
}