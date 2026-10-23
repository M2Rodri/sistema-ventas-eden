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

/** Carga el usuario desde la base para Spring Security. */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) throws UsernameNotFoundException {
        // En minúsculas: la base distingue mayúsculas y el celular autocapitaliza.
        String normalizado = usuario == null ? null : usuario.trim().toLowerCase();
        Usuario user = usuarioRepository.findByUsuario(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usuario));

        return new DetalleUsuario(
                user.getUsuario(),
                user.getPassword(),
                user.getActivo(),
                getAuthorities(user),
                user.getSesionesValidasDesde()
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Usuario user) {
        // El prefijo ROLE_ lo esperan hasRole/hasAnyRole de los controladores.
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));
    }
}
