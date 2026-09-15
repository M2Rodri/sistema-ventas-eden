package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.MensajeContacto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeContactoResponse {

    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private String asunto;
    private String mensaje;
    private Boolean atendido;
    private LocalDateTime fechaAtencion;
    private Long idUsuarioAtiende;
    private String nombreUsuarioAtiende;
    private LocalDateTime fechaEnvio;

    public MensajeContactoResponse(MensajeContacto m) {
        this.id = m.getId();
        this.nombre = m.getNombre();
        this.email = m.getEmail();
        this.telefono = m.getTelefono();
        this.asunto = m.getAsunto();
        this.mensaje = m.getMensaje();
        this.atendido = m.getAtendido();
        this.fechaAtencion = m.getFechaAtencion();
        this.idUsuarioAtiende = m.getUsuarioAtiende() != null ? m.getUsuarioAtiende().getId() : null;
        this.nombreUsuarioAtiende = m.getUsuarioAtiende() != null
                ? m.getUsuarioAtiende().getNombreCompleto() : null;
        this.fechaEnvio = m.getFechaEnvio();
        // La IP no se expone: sirve para rastrear abuso del formulario público,
        // no es información que el sistema necesite mostrar.
    }
}
