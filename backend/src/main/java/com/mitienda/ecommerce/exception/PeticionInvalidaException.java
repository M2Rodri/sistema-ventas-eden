package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** 400: petición mal formada o dato inválido que no es una regla de negocio. */
public class PeticionInvalidaException extends ApiException {

    public PeticionInvalidaException(String codigo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }
}
