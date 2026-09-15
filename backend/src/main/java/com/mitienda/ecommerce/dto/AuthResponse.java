package com.mitienda.ecommerce.dto;

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

    /**
     * Nombre del rol como texto plano ("ADMIN", "EMPLEADO").
     * Antes era el enum Role. Se deja como String a propósito: al normalizar
     * los roles en tabla, serializar la entidad produciría un objeto
     * {id, nombre, descripcion} y rompería al frontend, que espera
     * response.role === "ADMIN". El contrato de la API no cambia.
     */
    private String role;

    public AuthResponse(String token, Long id, String nombre, String apellido, String email, String role) {
        this.token = token;
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.role = role;
    }
}
