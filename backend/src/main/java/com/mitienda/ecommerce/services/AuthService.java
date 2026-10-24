package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.exception.AccesoProhibidoException;
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

/** Inicio de sesión, renovación y corte de sesiones. No hay registro público: las altas las hace el ADMIN. */
@Service
// Lectura en transacción: con open-in-view=false no hay sesión de Hibernate fuera de ella.
@Transactional(readOnly = true)
public class AuthService {
    static final String MENSAJE_APP_SOLO_ADMIN = "La app móvil es solo para administradores. Usá la versión web.";

    private final UsuarioRepository usuarioRepository;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final RegistroAuditoria registroAuditoria;

    private final LimiteIntentosLogin limiteIntentos;

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

    /** Login de un usuario del personal. */
    public AuthResponse login(LoginRequest request) {
        // Si las credenciales fallan se lanza AuthenticationException (401). El intento fallido se audita con su IP.
        // Usuario en minúsculas: la base distingue mayúsculas y el celular autocapitaliza.
        String usuarioNormalizado = request.getUsuario() == null
                ? null : request.getUsuario().trim().toLowerCase();

        // Cinco fallos en 15 minutos bloquean 15 minutos (429); se revisa antes de la clave para no permitir seguir probando.
        limiteIntentos.verificar(usuarioNormalizado);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usuarioNormalizado, request.getPassword())
            );
        } catch (RuntimeException e) {
            registroAuditoria.registrarPara(null, "LOGIN_FALLIDO", "usuarios", null,
                    "Intento fallido con el usuario " + request.getUsuario());
            // Solo cuentan las credenciales malas, no los errores de base de datos.
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

        // El rol viaja como texto ("ADMIN", "EMPLEADO").
        String nombreRol = user.getRoleName();
        // La app móvil es solo para administradores: el Empleado usa la web.
        if (JwtUtil.esPerfilMovil(request.getPerfilSesion()) && !"ADMIN".equalsIgnoreCase(nombreRol)) {
            registroAuditoria.registrarPara(user.getId(), "LOGIN_RECHAZADO", "usuarios", user.getId(),
                    "Intento de ingreso a la app móvil con el rol " + nombreRol);
            throw new AccesoProhibidoException("APP_SOLO_ADMIN", MENSAJE_APP_SOLO_ADMIN);
        }
        String token = jwtUtil.generateToken(user.getUsuario(), nombreRol, request.getPerfilSesion());

        // registrarPara: la autenticación aún no está en el SecurityContext, así que el usuario se pasa a mano.
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

    /** Renueva el token de una sesión viva (relee al usuario: baja y rol vigentes); si falla, 401. */
    public AuthResponse renovar(String authorization) {
        SesionVerificada sesion = verificarSesion(authorization);
        Usuario user = sesion.usuario();

        String nombreRol = user.getRoleName();
        // Un administrador al que le quitaron el rol pierde también la sesión de la app.
        if (jwtUtil.esSesionMovil(sesion.token()) && !"ADMIN".equalsIgnoreCase(nombreRol)) {
            throw new BadCredentialsException(MENSAJE_APP_SOLO_ADMIN);
        }
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

    /** Cierra todas las sesiones del usuario del token, incluida la actual; sus tokens anteriores dejan de servir. */
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

    /** Verifica el token de la cabecera: auténtico, vigente, usuario activo y sesión no cortada; si no, 401. */
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
