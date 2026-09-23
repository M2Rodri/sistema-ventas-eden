package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Resuelve el usuario autenticado de la petición actual a partir del JWT.
 *
 * Antes Venta, Compra y el ajuste manual de inventario recibían el id del
 * usuario como parámetro puesto por el cliente (idUsuario en el body o la
 * query), lo que permitía firmar cualquiera de esas operaciones con el id de
 * otra persona. El único origen posible ahora es el token: JwtAuthFilter deja
 * el email autenticado en el SecurityContext, y acá se resuelve contra la
 * tabla usuarios.
 *
 * Extraído de RegistroAuditoria, que tenía este mismo mecanismo pero privado
 * y sin reutilizar.
 */
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

        String email = null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails detalles) {
            email = detalles.getUsername();
        } else if (principal instanceof String texto && !"anonymousUser".equals(texto)) {
            email = texto;
        }

        if (email == null) {
            return null;
        }
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    /**
     * Igual que {@link #obtener()}, pero para operaciones que exigen un
     * usuario conocido (venta, compra, ajuste de inventario). Los endpoints
     * que las exponen ya exigen autenticación, así que esto solo dispara si
     * el token es válido pero el email ya no corresponde a ningún usuario.
     */
    public Usuario obtenerRequerido() {
        Usuario usuario = obtener();
        if (usuario == null) {
            throw new RuntimeException("No se pudo determinar el usuario autenticado");
        }
        return usuario;
    }

    /**
     * True solo si quien hace la petición es ADMIN. False para EMPLEADO y
     * para cualquiera sin sesión (incluida la tienda pública, que lee
     * /api/productos sin login).
     *
     * Se usa para no incluir costoReferencial (ni nada calculado a partir de
     * él, como el valor total del inventario) en respuestas que un EMPLEADO
     * o un visitante anónimo puedan recibir. El rol EMPLEADO existe
     * justamente para no ver costos ni márgenes.
     */
    public boolean esAdmin() {
        Usuario usuario = obtener();
        return usuario != null && "ADMIN".equals(usuario.getRoleName());
    }
}
