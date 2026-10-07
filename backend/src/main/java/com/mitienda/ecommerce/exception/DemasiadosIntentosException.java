package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Demasiados intentos seguidos (hoy, de inicio de sesión): 429 con el formato único de error. */
public class DemasiadosIntentosException extends ApiException {

    public DemasiadosIntentosException(String codigo, String mensaje) {
        super(HttpStatus.TOO_MANY_REQUESTS, codigo, mensaje);
    }
}
