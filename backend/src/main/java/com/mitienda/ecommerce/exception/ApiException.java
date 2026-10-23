package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Error esperado de la API: código HTTP y código propio (VENTA_NO_ENCONTRADA...); cada tipo tiene su subclase. */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus estado;
    private final String codigo;

    protected ApiException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
