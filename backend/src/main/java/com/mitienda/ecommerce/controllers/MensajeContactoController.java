package com.mitienda.ecommerce.controllers;

import com.mitienda.ecommerce.dto.MensajeContactoRequest;
import com.mitienda.ecommerce.dto.MensajeContactoResponse;
import com.mitienda.ecommerce.services.MensajeContactoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Consultas enviadas desde la tienda.
 *
 * El POST es público (la tienda no tiene login); todo lo demás requiere
 * ADMIN o EMPLEADO.
 */
@RestController
@RequestMapping("/api/mensajes-contacto")
@CrossOrigin(origins = "http://localhost:3000")
public class MensajeContactoController {

    private final MensajeContactoService mensajeContactoService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public MensajeContactoController(MensajeContactoService mensajeContactoService) {
        this.mensajeContactoService = mensajeContactoService;
    }


    /**
     * POST /api/mensajes-contacto
     * Enviar una consulta desde la tienda. Público.
     */
    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> enviarMensaje(@Valid @RequestBody MensajeContactoRequest request,
                                           HttpServletRequest http) {
        try {
            MensajeContactoResponse creado =
                    mensajeContactoService.registrarMensaje(request, obtenerIp(http));
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<MensajeContactoResponse>> getAllMensajes() {
        return ResponseEntity.ok(mensajeContactoService.getAllMensajes());
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<MensajeContactoResponse>> getPendientes() {
        return ResponseEntity.ok(mensajeContactoService.getMensajesPendientes());
    }

    @GetMapping("/pendientes/cantidad")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<Map<String, Long>> countPendientes() {
        return ResponseEntity.ok(Map.of("pendientes", mensajeContactoService.countPendientes()));
    }

    @PatchMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<?> marcarAtendido(@PathVariable Long id, @RequestParam Long idUsuario) {
        try {
            return ResponseEntity.ok(mensajeContactoService.marcarAtendido(id, idUsuario));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMensaje(@PathVariable Long id) {
        try {
            mensajeContactoService.deleteMensaje(id);
            return ResponseEntity.ok(Map.of("message", "Mensaje eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** Toma la IP real cuando la petición pasó por un proxy. */
    private String obtenerIp(HttpServletRequest http) {
        String reenviada = http.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
