package com.mitienda.ecommerce.dto;

import com.mitienda.ecommerce.models.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de autenticación
 * Contiene el token JWT y datos del usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private Role role;

    public AuthResponse(String token, Long id, String nombre, String apellido, String email, Role role) {
        this.token = token;
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.role = role;
    }
}
