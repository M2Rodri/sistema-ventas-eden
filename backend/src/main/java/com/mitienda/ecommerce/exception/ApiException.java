package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Error esperado de la API: lleva el código HTTP y un código propio del error
 * (VENTA_NO_ENCONTRADA, STOCK_INSUFICIENTE...). El manejador global lo convierte
 * en la respuesta con el formato único; ver {@link RespuestaError}.
 *
 * No se usa directamente: cada tipo de error tiene su subclase, que fija el
 * código HTTP.
 */
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
