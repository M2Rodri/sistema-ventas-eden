package com.mitienda.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta de autenticación: token JWT y datos del usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String nombre;
    private String apellido;
    private String usuario;

    /** Rol como texto plano ("ADMIN", "EMPLEADO"), que es lo que espera el frontend. */
    private String role;

    public AuthResponse(String token, Long id, String nombre, String apellido, String usuario, String role) {
        this.token = token;
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.usuario = usuario;
        this.role = role;
    }
}
