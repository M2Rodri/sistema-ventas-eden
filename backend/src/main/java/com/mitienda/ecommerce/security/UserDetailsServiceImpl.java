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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
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
