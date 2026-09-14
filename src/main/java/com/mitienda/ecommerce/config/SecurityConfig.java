package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.security.JwtAuthFilter;
import com.mitienda.ecommerce.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    private final JwtAuthFilter jwtAuthFilter;

    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthFilter jwtAuthFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(@NonNull HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // PÚBLICOS (sin autenticación)
                        // ========================================
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/uploads/**",
                                // /error debe ser público: en Spring Security 6 el filtro
                                // de autorización también se aplica al despacho de tipo
                                // ERROR. Sin este permitAll, cualquier 404 o 500 se
                                // convierte en un 403 con cuerpo vacío y el error real
                                // nunca llega al navegador.
                                "/error"
                        ).permitAll()
                        
                        // ========================================
                        // PROMOCIONES - Endpoints públicos para tienda virtual
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/promociones/vigentes").permitAll() // Tienda virtual
                        .requestMatchers(HttpMethod.GET, "/api/promociones/activas").permitAll() // Tienda virtual
                        .requestMatchers(HttpMethod.GET, "/api/promociones/{id}").permitAll() // Detalle de promocion
                        .requestMatchers("/api/promociones/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO") // Gestión ADMIN/EMPLEADO
                        
                        // ========================================
                        // IMÁGENES DE PRODUCTO
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/imagenes-producto/**").permitAll() // Lectura pública
                        .requestMatchers("/api/imagenes-producto/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO") // Escritura ADMIN/EMPLEADO
                        
                        // ========================================
                        // CONTACTO - la tienda puede enviar consultas
                        // ========================================
                        // Único endpoint de escritura público del sistema: la
                        // tienda no tiene login y este es el único canal por el
                        // que un cliente puede comunicarse con el negocio.
                        // Leer y gestionar los mensajes sigue siendo del personal.
                        .requestMatchers(HttpMethod.POST, "/api/mensajes-contacto").permitAll()
                        .requestMatchers("/api/mensajes-contacto/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO")

                        // ========================================
                        // DATOS DEL NEGOCIO - públicos para la tienda
                        // ========================================
                        // Solo las claves "negocio_*" (nombre, dirección, teléfono,
                        // horario). El resto de /api/configuracion sigue siendo ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/configuracion/negocio").permitAll()

                        // ========================================
                        // CATEGORÍAS
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll() // Lectura pública
                        .requestMatchers("/api/categorias/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO") // Escritura ADMIN/EMPLEADO
                        
                        // ========================================
                        // PRODUCTOS
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll() // Lectura pública
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
                        // La tienda publica necesita leer la multimedia para el visor 3D,
                        // pero antes esta regla no filtraba el metodo: dejaba abiertos sin
                        // token el POST de subida de modelos, el PUT y el DELETE. Cualquiera
                        // que alcanzara la API podia subir archivos o borrar registros.
                        // Mismo patron que productos: lectura publica, escritura solo ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/multimedia-productos/**").permitAll()
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