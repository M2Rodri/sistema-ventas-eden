package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de usuario (sin password)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String usuario;
    private String telefono;
    private String direccion;
    /** Nombre del rol como texto ("ADMIN", "EMPLEADO"). Ver la nota en AuthResponse. */
    private String role;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad Usuario
    public UsuarioResponse(com.mitienda.ecommerce.models.Usuario user) {
        this.id = user.getId();
        this.nombre = user.getNombre();
        this.apellido = user.getApellido();
        this.nombreCompleto = user.getNombreCompleto();
        this.usuario = user.getUsuario();
        this.telefono = user.getTelefono();
        this.direccion = user.getDireccion();
        this.role = user.getRoleName();
        this.activo = user.getActivo();
        this.fechaCreacion = user.getFechaCreacion();
        this.fechaActualizacion = user.getFechaActualizacion();
    }
}