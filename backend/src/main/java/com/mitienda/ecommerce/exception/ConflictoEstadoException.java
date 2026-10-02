package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** 409: la operación choca con el estado actual del recurso (compra ya confirmada, venta ya cancelada...). */
public class ConflictoEstadoException extends ApiException {

    public ConflictoEstadoException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
