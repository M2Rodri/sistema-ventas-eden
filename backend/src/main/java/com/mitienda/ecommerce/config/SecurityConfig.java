package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.exception.RespuestaError;
import com.mitienda.ecommerce.security.JwtAuthFilter;
import com.mitienda.ecommerce.security.UserDetailsServiceImpl;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;

    private final JwtAuthFilter jwtAuthFilter;

    private final CorsConfigurationSource corsConfigurationSource;

    private final AlmacenArchivos almacen;

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
        // /uploads/** solo se sirve para el disco local (desarrollo); con Supabase Storage exige ADMIN o EMPLEADO.
        AuthorizationManager<RequestAuthorizationContext> accesoUploads = almacen.esExterno()
                ? AuthorityAuthorizationManager.hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                : (autenticacion, contexto) -> new AuthorizationDecision(true);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(
                                "/api/v1/auth/**",
                                // Salud: el monitoreo del despliegue la consulta sin login.
                                "/api/v1/salud",
                                // /error debe ser público: sin esto, cualquier 404 o 500 llega como 403 sin cuerpo.
                                "/error"
                        ).permitAll()

                        // Archivos del disco del servidor (ver accesoUploads, arriba).
                        .requestMatchers("/uploads/**").access(accesoUploads)

                        // Imágenes y categorías: toda la ruta exige ADMIN o EMPLEADO; subir, borrar y elegir la foto principal es solo ADMIN.
                        .requestMatchers(HttpMethod.POST, "/api/v1/imagenes-producto/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/imagenes-producto/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/imagenes-producto/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/v1/imagenes-producto/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/v1/categorias/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")

                        // Productos: EMPLEADO puede verlos pero no crearlos ni editarlos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/v1/productos/**").hasAuthority("ROLE_ADMIN") // Escritura solo ADMIN

                        // Inventario
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventario").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventario/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")
                        .requestMatchers("/api/v1/inventario/**").hasAuthority("ROLE_ADMIN") // Modificación solo ADMIN

                        // Reportes: solo ADMIN, también por URL por si una ruta nueva olvida la anotación.
                        .requestMatchers("/api/v1/reportes/**").hasAuthority("ROLE_ADMIN")
                        
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 401 para sesión inválida y 403 para rol insuficiente, para que el frontend los distinga.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                RespuestaError.escribir(response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                                        "No autenticado o sesión inválida"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                RespuestaError.escribir(response, HttpStatus.FORBIDDEN, "SIN_PERMISO",
                                        "No tenés permiso para esta acción"))
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