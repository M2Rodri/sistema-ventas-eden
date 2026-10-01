package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.security.JwtAuthFilter;
import com.mitienda.ecommerce.security.UserDetailsServiceImpl;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    private final JwtAuthFilter jwtAuthFilter;

    private final CorsConfigurationSource corsConfigurationSource;

    private final AlmacenArchivos almacen;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthFilter jwtAuthFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          AlmacenArchivos almacen) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.almacen = almacen;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(@NonNull HttpSecurity http) throws Exception {
        // /uploads/** solo se sirve desde este servidor para archivos del disco local:
        // los de desarrollo y los registros viejos. Cuando los archivos viven en Supabase
        // Storage (producción), nada nuevo se pide por esta ruta y se cierra: exige sesión
        // de ADMIN o EMPLEADO. En desarrollo con disco local sigue abierta, porque las
        // pantallas muestran las fotos con <img>/<a href>, que no pueden mandar el token.
        AuthorizationManager<RequestAuthorizationContext> accesoUploads = almacen.esExterno()
                ? AuthorityAuthorizationManager.hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                : (autenticacion, contexto) -> new AuthorizationDecision(true);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // PÚBLICOS (sin autenticación)
                        // ========================================
                        .requestMatchers(
                                "/api/auth/**",
                                // Ruta de salud para el monitoreo de despliegue (Render, etc.):
                                // tiene que responder sin login para que la plataforma pueda
                                // confirmar que el servicio sigue vivo.
                                "/api/v1/salud",
                                // /error debe ser público: en Spring Security 6 el filtro
                                // de autorización también se aplica al despacho de tipo
                                // ERROR. Sin este permitAll, cualquier 404 o 500 se
                                // convierte en un 403 con cuerpo vacío y el error real
                                // nunca llega al navegador. No expone datos del negocio.
                                "/error"
                        ).permitAll()

                        // Archivos del disco del servidor (ver accesoUploads, arriba).
                        .requestMatchers("/uploads/**").access(accesoUploads)

                        // ========================================
                        // PROMOCIONES, IMÁGENES DE PRODUCTO, CATEGORÍAS
                        // ========================================
                        // Antes tenían lectura pública para la tienda virtual. La tienda
                        // quedó fuera del alcance del proyecto: nada en el panel
                        // ADMIN/EMPLEADO ni en la app llama a estos endpoints sin sesión,
                        // así que ahora toda la ruta requiere ADMIN o EMPLEADO.
                        .requestMatchers("/api/promociones/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/imagenes-producto/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/categorias/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")

                        // ========================================
                        // PRODUCTOS
                        // ========================================
                        // Lectura pública era para la tienda virtual (fuera de alcance);
                        // el panel ADMIN/EMPLEADO siempre manda su token. EMPLEADO puede
                        // ver productos pero no crearlos/editarlos.
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/productos/**").hasAuthority("ROLE_ADMIN") // Escritura solo ADMIN

                        // ========================================
                        // INVENTARIO
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/inventario").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/inventario/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/inventario/**").hasAuthority("ROLE_ADMIN") // Modificación solo ADMIN

                        // ========================================
                        // MULTIMEDIA PRODUCTOS
                        // ========================================
                        // Misma razón que productos: la lectura pública era para el visor
                        // 3D de la tienda, que quedó fuera de alcance. Mismo nivel que
                        // imágenes de producto: lectura ADMIN/EMPLEADO, escritura ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/multimedia-productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/multimedia-productos/**").hasAuthority("ROLE_ADMIN")

                        // ========================================
                        // REPORTES
                        // ========================================
                        .requestMatchers("/api/reportes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        
                        // ========================================
                        // TODO LO DEMÁS requiere autenticación
                        // ========================================
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Sin esto, Spring Security responde 403 tanto para "no hay token
                // válido" como para "hay token válido pero rol insuficiente", y el
                // frontend no puede distinguir una sesión vencida de un EMPLEADO
                // pidiendo algo de ADMIN. Separados: 401 es lo primero (te vas al
                // login), 403 es lo segundo (esa pantalla no te muestra ese dato).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"error\":\"No autenticado o sesión inválida\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"error\":\"No tenés permiso para esta acción\"}");
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(@NonNull AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}