package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.security.JwtUtil;
import com.mitienda.ecommerce.security.LimiteIntentosLogin;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación.
 *
 * Solo maneja el login. El registro público se eliminó: la tienda web es una
 * vitrina sin inicio de sesión, así que los únicos usuarios del sistema son el
 * dueño y los vendedores, y las altas las hace el ADMIN desde la gestión de
 * usuarios. Dejar abierto /api/v1/auth/register habría permitido que cualquiera
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

    private final LimiteIntentosLogin limiteIntentos;

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
                       RegistroAuditoria registroAuditoria,
                       LimiteIntentosLogin limiteIntentos) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.registroAuditoria = registroAuditoria;
        this.limiteIntentos = limiteIntentos;
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
        // Normalizado a minúsculas: la comparación en la base distingue
        // mayúsculas de minúsculas, y el celular autocapitaliza seguido.
        String usuarioNormalizado = request.getUsuario() == null
                ? null : request.getUsuario().trim().toLowerCase();

        // Cinco fallos en 15 minutos bloquean al usuario 15 minutos (429). Se revisa
        // antes de mirar la contraseña, para que un usuario bloqueado no pueda seguir
        // probando claves.
        limiteIntentos.verificar(usuarioNormalizado);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usuarioNormalizado, request.getPassword())
            );
        } catch (RuntimeException e) {
            registroAuditoria.registrarPara(null, "LOGIN_FALLIDO", "usuarios", null,
                    "Intento fallido con el usuario " + request.getUsuario());
            // Solo cuentan las credenciales malas: un error de base de datos o de otro tipo
            // no es culpa de quien intenta entrar.
            if (e instanceof AuthenticationException && limiteIntentos.registrarFallo(usuarioNormalizado)) {
                registroAuditoria.registrarPara(null, "LOGIN_BLOQUEADO", "usuarios", null,
                        "Usuario " + request.getUsuario() + " bloqueado " + LimiteIntentosLogin.BLOQUEO.toMinutes()
                                + " minutos por " + LimiteIntentosLogin.MAX_FALLOS + " intentos fallidos");
            }
            throw e;
        }
        limiteIntentos.limpiar(usuarioNormalizado);

        Usuario user = usuarioRepository.findByUsuario(usuarioNormalizado)
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
        String token = jwtUtil.generateToken(user.getUsuario(), nombreRol, request.getPerfilSesion());

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
                user.getUsuario(),
                nombreRol
        );
    }

    /**
     * Renueva el token de una sesión que sigue viva.
     *
     * La ruta /api/v1/auth/** es pública, así que acá no se confía en el filtro: el propio
     * token se verifica (firma y vencimiento). Se vuelve a leer al usuario en la base: si lo
     * dieron de baja se rechaza, y el rol del token nuevo es el vigente hoy, no el del viejo.
     * Cualquier falla responde 401 (AuthenticationException) y obliga a iniciar sesión de nuevo.
     */
    public AuthResponse renovar(String authorization) {
        SesionVerificada sesion = verificarSesion(authorization);
        Usuario user = sesion.usuario();

        String nombreRol = user.getRoleName();
        String nuevoToken;
        try {
            nuevoToken = jwtUtil.renovarToken(sesion.token(), nombreRol);
        } catch (JwtException e) {
            throw new BadCredentialsException("Token inválido o vencido");
        }

        return new AuthResponse(
                nuevoToken,
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getUsuario(),
                nombreRol
        );
    }

    /**
     * Cierra todas las sesiones abiertas del usuario del token, en cualquier dispositivo, incluida
     * la actual: desde este momento los tokens anteriores se rechazan y no se pueden renovar.
     * Sirve cuando se pierde o le roban el celular a alguien.
     */
    @Transactional
    public void cerrarSesiones(String authorization) {
        Usuario user = verificarSesion(authorization).usuario();
        user.setSesionesValidasDesde(System.currentTimeMillis());
        usuarioRepository.save(user);
        registroAuditoria.registrarPara(user.getId(), "CERRAR_SESIONES", "usuarios", user.getId(),
                "Cierre de todas las sesiones de " + user.getUsuario());
    }

    /** Una sesión que pasó todas las verificaciones: el token auténtico y el usuario que lo tiene. */
    private record SesionVerificada(String token, Usuario usuario) {}

    /**
     * Verifica el token de la cabecera Authorization: que exista y sea auténtico y vigente, que su
     * usuario siga existiendo y activo, y que la sesión no haya sido cortada. Cualquier falla lanza
     * una AuthenticationException, que se responde con 401.
     */
    private SesionVerificada verificarSesion(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("Falta el token");
        }
        String token = authorization.substring(7);

        String usuario;
        long inicioSesion;
        try {
            usuario = jwtUtil.extractUsuario(token);
            inicioSesion = jwtUtil.extractInicioSesion(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Token inválido o vencido");
        }

        Usuario user = usuarioRepository.findByUsuario(usuario)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        if (Boolean.FALSE.equals(user.getActivo())) {
            throw new DisabledException("Usuario inactivo");
        }
        Long corte = user.getSesionesValidasDesde();
        if (corte != null && inicioSesion < corte) {
            throw new BadCredentialsException("La sesión fue cerrada");
        }
        return new SesionVerificada(token, user);
    }
}
