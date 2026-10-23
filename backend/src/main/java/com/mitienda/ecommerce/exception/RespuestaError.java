package com.mitienda.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

/** Formato único de error: { "error": { "codigo", "mensaje", "campos" } }; "campos" solo en errores de validación. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaError(Detalle error) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detalle(String codigo, String mensaje, Map<String, String> campos) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    public static RespuestaError de(String codigo, String mensaje) {
        return new RespuestaError(new Detalle(codigo, mensaje, null));
    }

    public static RespuestaError de(String codigo, String mensaje, Map<String, String> campos) {
        return new RespuestaError(new Detalle(codigo, mensaje, campos));
    }

    /** Escribe el error en la respuesta, para los filtros y manejadores de seguridad que actúan antes del controlador. */
    public static void escribir(HttpServletResponse response, HttpStatus estado, String codigo, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(de(codigo, mensaje)));
    }
}
