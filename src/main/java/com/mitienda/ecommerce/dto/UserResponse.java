package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Role;
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
public class UserResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private String direccion;
    private Role role;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor desde entidad User
    public UserResponse(com.mitienda.ecommerce.models.User user) {
        this.id = user.getId();
        this.nombre = user.getNombre();
        this.apellido = user.getApellido();
        this.nombreCompleto = user.getNombreCompleto();
        this.email = user.getEmail();
        this.telefono = user.getTelefono();
        this.direccion = user.getDireccion();
        this.role = user.getRole();
        this.activo = user.getActivo();
        this.fechaCreacion = user.getFechaCreacion();
        this.fechaActualizacion = user.getFechaActualizacion();
    }
}