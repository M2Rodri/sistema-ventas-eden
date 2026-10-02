package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Rutas que solo existen en las pruebas: lanzan cada tipo de error para comprobar el
 * código HTTP y el formato de la respuesta sin depender de la base de datos.
 */
@RestController
@RequestMapping("/api/v1/prueba-errores")
class ControladorDePruebaErrores {

    record Datos(@NotBlank(message = "El nombre es obligatorio") String nombre,
                 @Min(value = 1, message = "El precio debe ser mayor a 0") int precio) {
    }

    @RequestMapping(value = "/no-encontrado", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    String noEncontrado() {
        throw new RecursoNoEncontradoException("VENTA_NO_ENCONTRADA", "Venta no encontrada con ID: 5");
    }

    @GetMapping("/peticion")
    String peticion() {
        throw new PeticionInvalidaException("ESTADO_INVALIDO", "Estado de venta inválido: XYZ");
    }

    @GetMapping("/conflicto")
    String conflicto() {
        throw new ConflictoEstadoException("COMPRA_YA_CONFIRMADA", "La compra ya está confirmada");
    }

    @GetMapping("/regla")
    String regla() {
        throw new ReglaNegocioException("STOCK_INSUFICIENTE", "Stock insuficiente para el producto 'Cama'");
    }

    @GetMapping("/interno")
    String interno() {
        throw new IllegalStateException("detalle interno: tabla ventas, columna x");
    }

    @GetMapping("/numero/{n}")
    String numero(@PathVariable Long n) {
        return "ok";
    }

    @PostMapping("/validar")
    String validar(@Valid @RequestBody Datos datos) {
        return "ok";
    }

    @GetMapping("/solo-admin")
    @PreAuthorize("hasRole('ADMIN')")
    String soloAdmin() {
        return "ok";
    }
}
