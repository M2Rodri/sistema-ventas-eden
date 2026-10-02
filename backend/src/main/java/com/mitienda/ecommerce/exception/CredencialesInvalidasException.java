package com.mitienda.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** 401: usuario inexistente, contraseña incorrecta o cuenta desactivada al iniciar sesión. */
public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Credenciales inválidas");
    }
}
