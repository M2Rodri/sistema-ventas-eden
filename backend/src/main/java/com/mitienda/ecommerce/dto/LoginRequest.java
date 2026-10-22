package com.mitienda.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para peticiones de login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /**
     * Opcional. Desde dónde se inicia sesión, para fijar el tope de la sesión:
     * MOVIL_CON_CANDADO (la app, con huella o bloqueo de pantalla), MOVIL_SIN_CANDADO (la app, sin
     * ningún candado). Vacío o cualquier otro valor = la web. Es una política, no una barrera:
     * lo que protege un celular perdido es el candado y el corte de sesiones.
     */
    private String perfilSesion;
}
