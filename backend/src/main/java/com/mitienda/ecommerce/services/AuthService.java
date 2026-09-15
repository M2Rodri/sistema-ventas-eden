package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.security.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación.
 *
 * Solo maneja el login. El registro público se eliminó: la tienda web es una
 * vitrina sin inicio de sesión, así que los únicos usuarios del sistema son el
 * dueño y los vendedores, y las altas las hace el ADMIN desde la gestión de
 * usuarios. Dejar abierto /api/auth/register habría permitido que cualquiera
 * se creara una cuenta contra la API.
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final RegistroAuditoria registroAuditoria;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public AuthService(UsuarioRepository usuarioRepository,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       RegistroAuditoria registroAuditoria) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.registroAuditoria = registroAuditoria;
    }


    /**
     * Login de un usuario del personal.
     */
    public AuthResponse login(LoginRequest request) {
        // Valida credenciales. Si fallan, lanza AuthenticationException y el
        // controlador responde 401.
        //
        // El intento fallido tambien se audita: saber que alguien probo entrar
        // con una contrasena equivocada, y desde que IP, es justamente el tipo
        // de cosa para la que sirve una tabla de auditoria.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (RuntimeException e) {
            registroAuditoria.registrarPara(null, "LOGIN_FALLIDO", "usuarios", null,
                    "Intento fallido con el correo " + request.getEmail());
            throw e;
        }

        Usuario user = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new org.springframework.security.core.userdetails
                        .UsernameNotFoundException("Usuario no encontrado"));

        if (Boolean.FALSE.equals(user.getActivo())) {
            registroAuditoria.registrarPara(user.getId(), "LOGIN_RECHAZADO", "usuarios", user.getId(),
                    "Intento de ingreso de un usuario dado de baja");
            throw new DisabledException("Usuario inactivo");
        }

        // El rol viaja en el token como texto ("ADMIN", "EMPLEADO"), igual que
        // antes de normalizarlo en tabla. El contrato del token no cambia.
        String nombreRol = user.getRoleName();
        String token = jwtUtil.generateToken(user.getEmail(), nombreRol);

        // registrarPara y no registrar: en este punto la autenticacion todavia
        // no quedo guardada en el SecurityContext, asi que el usuario hay que
        // pasarlo a mano.
        registroAuditoria.registrarPara(user.getId(), "LOGIN", "usuarios", user.getId(),
                "Ingreso al sistema como " + nombreRol);

        return new AuthResponse(
                token,
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getEmail(),
                nombreRol
        );
    }
}
