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
                                // /error debe ser público: en Spring Security 6 el filtro
                                // de autorización también se aplica al despacho de tipo
                                // ERROR. Sin este permitAll, cualquier 404 o 500 se
                                // convierte en un 403 con cuerpo vacío y el error real
                                // nunca llega al navegador. No expone datos del negocio.
                                "/error",
                                // Archivos estáticos (imágenes de producto, modelos 3D,
                                // comprobantes de pago). El propio panel ADMIN/EMPLEADO los
                                // muestra con <img>/<a href> directos al backend, que no
                                // pueden mandar el header Authorization, así que esta ruta
                                // tiene que seguir sin login aunque la tienda haya quedado
                                // fuera de alcance.
                                "/uploads/**"
                        ).permitAll()

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