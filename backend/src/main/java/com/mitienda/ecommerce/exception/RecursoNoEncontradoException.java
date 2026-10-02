package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso pedido no existe (también en PUT, PATCH y DELETE). */
public class RecursoNoEncontradoException extends ApiException {

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(HttpStatus.NOT_FOUND, codigo, mensaje);
    }
}
