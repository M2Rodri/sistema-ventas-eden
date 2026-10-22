package com.mitienda.ecommerce.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Usuario de Spring Security que además trae el corte de sesiones.
 *
 * Así el filtro JWT, que ya carga al usuario en cada petición, puede comparar el inicio de la
 * sesión del token con el corte sin hacer otra consulta a la base.
 */
public class DetalleUsuario extends User {

    private final Long sesionesValidasDesde;

    public DetalleUsuario(String usuario, String password, boolean activo,
                          Collection<? extends GrantedAuthority> autoridades, Long sesionesValidasDesde) {
        super(usuario, password, activo, true, true, true, autoridades);
        this.sesionesValidasDesde = sesionesValidasDesde;
    }

    /** Instante desde el que valen las sesiones (ms desde 1970), o null si nunca se cortaron. */
    public Long getSesionesValidasDesde() {
        return sesionesValidasDesde;
    }

    /** true si una sesión que empezó en {@code inicioMs} fue cortada. */
    public boolean sesionCortada(long inicioMs) {
        return sesionesValidasDesde != null && inicioMs < sesionesValidasDesde;
    }
}
