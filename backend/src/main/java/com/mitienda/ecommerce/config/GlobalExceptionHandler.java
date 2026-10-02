package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.exception.ApiException;
import com.mitienda.ecommerce.exception.RespuestaError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Único lugar donde una excepción se convierte en respuesta de error, con el
 * formato único de la API (ver {@link RespuestaError}).
 *
 * - Las {@link ApiException} (no encontrado, conflicto, regla de negocio...) llevan su
 *   propio código HTTP y código de error.
 * - Los errores que Spring detecta antes de llegar al controlador (validación de campos,
 *   JSON mal formado, tipo de parámetro incorrecto, método no permitido...) se resuelven
 *   en la clase base y se les da el mismo formato en {@link #handleExceptionInternal}.
 * - 401 y 403 de las anotaciones @PreAuthorize también pasan por acá; los de las reglas de
 *   URL los escribe SecurityConfig con el mismo formato.
 * - Cualquier otra excepción es un 500 sin detalles internos ni traza: el detalle va solo al log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<RespuestaError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getEstado()).body(RespuestaError.de(ex.getCodigo(), ex.getMessage()));
    }

    /** @Valid sobre un @RequestBody: 400 con el detalle de cada campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String mensaje = campos.isEmpty()
                ? "Los datos enviados no son válidos"
                : String.join(". ", campos.values());
        return ResponseEntity.badRequest().body(RespuestaError.de("VALIDACION", mensaje, campos));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> handleSinPermiso(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(RespuestaError.de("SIN_PERMISO", "No tenés permiso para esta acción"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespuestaError> handleNoAutenticado(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RespuestaError.de("NO_AUTENTICADO", "No autenticado o sesión inválida"));
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(RespuestaError.de("PETICION_INVALIDA", "El archivo supera el tamaño máximo permitido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> handleIntegridad(DataIntegrityViolationException ex) {
        log.warn("Conflicto de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RespuestaError.de("CONFLICTO_DATOS",
                "La operación choca con datos que ya existen o están en uso"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> handleInterno(Exception ex) {
        log.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaError.de("ERROR_INTERNO", "Ocurrió un error inesperado. Intentá de nuevo en un momento."));
    }

    /** Resto de errores de Spring MVC: se conserva su código HTTP y se les da el formato único. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus estado = HttpStatus.resolve(statusCode.value());
        String codigo;
        String mensaje;
        if (ex instanceof NoResourceFoundException) {
            codigo = "RUTA_NO_ENCONTRADA";
            mensaje = "La ruta pedida no existe";
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            codigo = "METODO_NO_PERMITIDO";
            mensaje = "El método HTTP no está permitido en esta ruta";
        } else if (ex instanceof HttpMediaTypeNotSupportedException) {
            codigo = "TIPO_NO_SOPORTADO";
            mensaje = "El tipo de contenido enviado no está soportado";
        } else if (statusCode.is4xxClientError()) {
            codigo = "PETICION_INVALIDA";
            mensaje = "La petición está mal formada o le falta algún dato";
        } else {
            log.error("Error interno de Spring MVC", ex);
            codigo = "ERROR_INTERNO";
            mensaje = "Ocurrió un error inesperado. Intentá de nuevo en un momento.";
            estado = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(estado != null ? estado : HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(RespuestaError.de(codigo, mensaje));
    }
}
