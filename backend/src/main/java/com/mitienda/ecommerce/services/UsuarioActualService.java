package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Resuelve el usuario autenticado a partir del JWT; es el único origen del vendedor de una operación. */
@Service
public class UsuarioActualService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioActualService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Usuario autenticado de la petición actual, o null si no hay sesión. */
    public Usuario obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String usuario = null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails detalles) {
            usuario = detalles.getUsername();
        } else if (principal instanceof String texto && !"anonymousUser".equals(texto)) {
            usuario = texto;
        }

        if (usuario == null) {
            return null;
        }
        return usuarioRepository.findByUsuario(usuario).orElse(null);
    }

    /** Como obtener(), pero falla si el token es válido y el usuario ya no existe. */
    public Usuario obtenerRequerido() {
        Usuario usuario = obtener();
        if (usuario == null) {
            throw new RuntimeException("No se pudo determinar el usuario autenticado");
        }
        return usuario;
    }

    /** True solo si quien pide es ADMIN; evita enviar costos y márgenes a un EMPLEADO. */
    public boolean esAdmin() {
        Usuario usuario = obtener();
        return usuario != null && "ADMIN".equals(usuario.getRoleName());
    }
}
