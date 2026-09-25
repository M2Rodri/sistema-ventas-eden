package com.mitienda.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String usuario = null;
        String jwt = null;

        // Extraer el token del header Authorization
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                usuario = jwtUtil.extractUsuario(jwt);
            } catch (Exception e) {
                logger.error("Error al extraer usuario del token: " + e.getMessage());
            }
        }

        // Validar el token y establecer la autenticación
        if (usuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(usuario);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                // Extraer el rol del token (viene como "ADMIN", "CLIENTE", "EMPLEADO")
                String roleFromToken = jwtUtil.extractRole(jwt);
                
                // Agregar el prefijo ROLE_ para Spring Security
                String authority = "ROLE_" + roleFromToken;
                
                // Aca habia un bloque de println que imprimia usuario, rol y ruta en
                // CADA peticion autenticada. Se quito: dejaba un rastro de quien
                // uso el sistema y para que en la consola del servidor, que no es
                // el lugar para eso. Lo que hay que auditar se guarda en la tabla
                // auditorias, con usuario, IP y fecha.

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}