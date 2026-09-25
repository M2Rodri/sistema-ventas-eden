package com.mitienda.ecommerce.security;

import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementación de UserDetailsService para Spring Security
 * Carga los datos del usuario desde la base de datos
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String usuario) throws UsernameNotFoundException {
        // Normalizado a minúsculas: la comparación en la base distingue
        // mayúsculas de minúsculas, y el celular autocapitaliza seguido.
        // Sin esto, "Juan" y "juan" serían usuarios distintos para el login.
        String normalizado = usuario == null ? null : usuario.trim().toLowerCase();
        Usuario user = usuarioRepository.findByUsuario(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usuario));

        return new org.springframework.security.core.userdetails.User(
                user.getUsuario(),
                user.getPassword(),
                user.getActivo(),
                true,
                true,
                true,
                getAuthorities(user)
        );
    }

    // Obtener authorities (roles) del usuario
    private Collection<? extends GrantedAuthority> getAuthorities(Usuario user) {
        // El nombre del rol ahora viene de la tabla 'roles' en lugar del enum.
        // El prefijo ROLE_ se mantiene porque es lo que esperan las reglas
        // hasRole('ADMIN') / hasAnyRole(...) de los controladores.
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));
    }
}
