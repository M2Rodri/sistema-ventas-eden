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

            // Además de que el token sea auténtico y no haya vencido: la cuenta tiene que seguir
            // activa (si dan de baja a alguien, su token deja de servir de inmediato) y su sesión
            // no puede haber sido cortada ("cerrar sesiones" o cambio de contraseña).
            if (userDetails.isEnabled()
                    && jwtUtil.validateToken(jwt, userDetails)
                    && !sesionCortada(jwt, userDetails)) {
                // El rol se toma de la base (los userDetails que se acaban de cargar), no del
                // token: si a alguien le cambian el rol, el cambio rige desde la próxima petición
                // y no recién cuando venza el token que ya tenía.

                // Aca habia un bloque de println que imprimia usuario, rol y ruta en
                // CADA peticion autenticada. Se quito: dejaba un rastro de quien
                // uso el sistema y para que en la consola del servidor, que no es
                // el lugar para eso. Lo que hay que auditar se guarda en la tabla
                // auditorias, con usuario, IP y fecha.

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

    /** true si el dueño de la cuenta cortó las sesiones después de que empezó la de este token. */
    private boolean sesionCortada(String jwt, UserDetails userDetails) {
        return userDetails instanceof DetalleUsuario detalle
                && detalle.sesionCortada(jwtUtil.extractInicioSesion(jwt));
    }
}