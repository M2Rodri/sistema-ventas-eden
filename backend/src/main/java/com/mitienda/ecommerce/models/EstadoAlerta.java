package com.mitienda.ecommerce.models;

/**
 * Enum para estados de alertas de inventario
 */
public enum EstadoAlerta {
    /** Hay que mirarla: el producto está bajo el mínimo. */
    PENDIENTE,
    /** Se resolvió sola: el producto se repuso por encima del mínimo. */
    ATENDIDA,
    /**
     * El dueño la marcó como atendida sin reponer: no se vuelve a avisar mientras el producto siga
     * bajo el mínimo. Si se repone y vuelve a bajar, avisa de nuevo.
     */
    ATENDIDA_MANUAL
}