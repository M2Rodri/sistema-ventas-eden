package com.mitienda.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    private final UserDetailsServiceImpl userDetailsService;

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

        if (usuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(usuario);

            // El token debe ser válido y la cuenta seguir activa (una baja corta el acceso de inmediato) y sin sesión cortada.
            if (userDetails.isEnabled()
                    && jwtUtil.validateToken(jwt, userDetails)
                    && !sesionCortada(jwt, userDetails)
                    && !appMovilSinPermiso(jwt, userDetails)) {
                // El rol sale de la base y no del token: un cambio de rol rige desde la próxima petición.

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** true si el token es de la app móvil y la cuenta ya no es de administrador (la app es solo para ellos). */
    private boolean appMovilSinPermiso(String jwt, UserDetails userDetails) {
        return jwtUtil.esSesionMovil(jwt)
                && userDetails.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** true si el dueño de la cuenta cortó las sesiones después de que empezó la de este token. */
    private boolean sesionCortada(String jwt, UserDetails userDetails) {
        return userDetails instanceof DetalleUsuario detalle
                && detalle.sesionCortada(jwtUtil.extractInicioSesion(jwt));
    }
}