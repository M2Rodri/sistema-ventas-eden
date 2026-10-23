package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** 403: la persona está autenticada pero no puede hacer esto sobre este recurso. */
public class AccesoProhibidoException extends ApiException {

    public AccesoProhibidoException(String codigo, String mensaje) {
        super(HttpStatus.FORBIDDEN, codigo, mensaje);
    }
}
