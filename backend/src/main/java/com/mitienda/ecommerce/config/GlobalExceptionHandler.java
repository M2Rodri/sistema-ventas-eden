package com.mitienda.ecommerce.config;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Las anotaciones de validación (@NotBlank, @DecimalMin, etc. sobre un
 * @Valid @RequestBody) fallan ANTES de que el método del controller llegue
 * a ejecutarse, así que el try/catch de cada endpoint nunca las atrapa: sin
 * esto, cualquier dato inválido le llegaba al usuario como un "Bad Request"
 * genérico en vez del motivo real (por ejemplo, "El costo referencial debe
 * ser mayor a 0"). Al ser global, cubre a todos los controllers por igual.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(". "));
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }
}
