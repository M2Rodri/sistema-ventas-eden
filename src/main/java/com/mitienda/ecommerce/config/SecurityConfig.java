package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.security.JwtAuthFilter;
import com.mitienda.ecommerce.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

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
                                "/uploads/**"
                        ).permitAll()
                        
                        // ========================================
                        // OFERTAS - Endpoints públicos para tienda virtual
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/ofertas/vigentes").permitAll() // Tienda virtual
                        .requestMatchers(HttpMethod.GET, "/api/ofertas/activas").permitAll() // Tienda virtual
                        .requestMatchers(HttpMethod.GET, "/api/ofertas/{id}").permitAll() // Detalle de oferta
                        .requestMatchers("/api/ofertas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO") // Gestión ADMIN/EMPLEADO
                        
                        // ========================================
                        // RESEÑAS - Endpoints públicos y de clientes
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/resenias/producto/{idProducto}").permitAll() // Reseñas aprobadas de producto (público)
                        .requestMatchers(HttpMethod.GET, "/api/resenias/producto/{idProducto}/promedio").permitAll() // Calificación promedio (público)
                        .requestMatchers(HttpMethod.POST, "/api/resenias").permitAll() // Crear reseña (público - luego requiere aprobación)
                        .requestMatchers("/api/resenias/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO") // Gestión ADMIN/EMPLEADO
                        
                        // ========================================
                        // IMÁGENES DE PRODUCTO
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/imagenes-producto/**").permitAll() // Lectura pública
                        .requestMatchers("/api/imagenes-producto/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_EMPLEADO") // Escritura ADMIN/EMPLEADO
                        
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
                        .requestMatchers("/api/multimedia-productos/**").permitAll()

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