package com.mitienda.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

/**
 * Formato único de error de la API, en todas las rutas:
 * <pre>
 * { "error": { "codigo": "VENTA_NO_ENCONTRADA", "mensaje": "texto para el usuario", "campos": { "campo": "detalle" } } }
 * </pre>
 * "campos" solo viene en los errores de validación de campos.
 *
 * Códigos genéricos, solo para errores globales: VALIDACION, PETICION_INVALIDA,
 * NO_AUTENTICADO, SIN_PERMISO, RUTA_NO_ENCONTRADA, METODO_NO_PERMITIDO,
 * TIPO_NO_SOPORTADO, ERROR_INTERNO. El resto son propios de cada error.
 */
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

    /**
     * Escribe el error directamente en la respuesta. Lo usan los filtros y los
     * manejadores de seguridad (401, 403, ruta sin versión), que actúan antes de
     * que exista un controlador y por eso no pasan por el manejador global.
     */
    public static void escribir(HttpServletResponse response, HttpStatus estado, String codigo, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(de(codigo, mensaje)));
    }
}
