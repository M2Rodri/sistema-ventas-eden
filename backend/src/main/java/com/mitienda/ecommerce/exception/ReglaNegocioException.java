package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** 422: la petición es válida pero viola una regla de negocio (stock insuficiente, pago mayor al saldo...). */
public class ReglaNegocioException extends ApiException {

    public ReglaNegocioException(String codigo, String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensaje);
    }
}
