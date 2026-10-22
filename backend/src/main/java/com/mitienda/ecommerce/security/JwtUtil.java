package com.mitienda.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidad para generar y validar tokens JWT
 */
@Component
public class JwtUtil {

    /** Perfil de sesión de la app móvil cuyo celular tiene un candado (huella o bloqueo de pantalla). */
    public static final String PERFIL_MOVIL_CON_CANDADO = "MOVIL_CON_CANDADO";
    /** Perfil de sesión de la app móvil cuyo celular no tiene ningún candado. */
    public static final String PERFIL_MOVIL_SIN_CANDADO = "MOVIL_SIN_CANDADO";

    @Value("${jwt.secret}")
    private String secret;

    /** Vida de cada token. */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Tope de una sesión desde el inicio de sesión, aunque se renueve el token: es el de la web.
     * Sin un tope, un token robado y renovado una y otra vez no vencería nunca.
     */
    @Value("${jwt.sesion-maxima:28800000}")
    private Long sesionMaxima;

    /** Tope para la app móvil cuyo celular tiene candado: la huella protege la sesión, puede ser más larga. */
    @Value("${jwt.sesion-maxima-movil:43200000}")
    private Long sesionMaximaMovil;

    /** Tope para la app móvil cuyo celular no tiene candado: sin protección extra, más corto. */
    @Value("${jwt.sesion-maxima-movil-sin-candado:14400000}")
    private Long sesionMaximaMovilSinCandado;

    // Generar clave secreta
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Extraer usuario del token
    public String extractUsuario(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extraer rol del token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // Extraer fecha de expiración
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /** Cuándo empezó la sesión del token (ms desde 1970). Los tokens viejos sin ese dato valen por su emisión. */
    public long extractInicioSesion(String token) {
        return inicioDeSesion(extractAllClaims(token));
    }

    // Extraer un claim específico
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extraer todos los claims - VERSIÓN COMPATIBLE
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Verificar si el token expiró
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Generar token para un usuario (SOBRECARGA - CON ROL), sesión de la web
    public String generateToken(String usuario, String role) {
        return generateToken(usuario, role, null);
    }

    /**
     * Genera el token de un inicio de sesión. El perfil dice desde dónde entra el usuario y fija el
     * tope de la sesión; null o desconocido = la web.
     */
    public String generateToken(String usuario, String role, String perfilSesion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        // Cuándo empezó la sesión (el inicio de sesión). Las renovaciones lo conservan,
        // y con él se calcula el tope de duración de la sesión y el corte de sesiones.
        claims.put("inicio", System.currentTimeMillis());
        // Tope de esta sesión según el perfil; las renovaciones lo conservan.
        claims.put("tope", topeDelPerfil(perfilSesion));
        return createToken(claims, usuario);
    }

    // Generar token para un usuario (SIN ROL - MANTENER COMPATIBILIDAD)
    public String generateToken(String usuario) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, usuario);
    }

    private long topeDelPerfil(String perfilSesion) {
        if (PERFIL_MOVIL_CON_CANDADO.equals(perfilSesion)) {
            return sesionMaximaMovil;
        }
        if (PERFIL_MOVIL_SIN_CANDADO.equals(perfilSesion)) {
            return sesionMaximaMovilSinCandado;
        }
        return sesionMaxima;
    }

    /**
     * Renueva un token que todavía es válido: mismo usuario y mismo inicio de sesión, con un
     * vencimiento nuevo y el rol vigente en la base (no el que traía el token viejo).
     * El vencimiento nunca pasa del tope de la sesión. Si el token ya venció o no es auténtico,
     * extractAllClaims lanza una JwtException; si la sesión llegó a su tope, se lanza
     * CredentialsExpiredException. En ambos casos hay que volver a iniciar sesión.
     */
    public String renovarToken(String token, String rolActual) {
        Claims actuales = extractAllClaims(token);
        long ahora = System.currentTimeMillis();
        long inicio = inicioDeSesion(actuales);
        long tope = actuales.get("tope") instanceof Number numero ? numero.longValue() : sesionMaxima;
        long limite = inicio + tope;
        if (ahora >= limite) {
            throw new CredentialsExpiredException("La sesión alcanzó su duración máxima");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", rolActual);
        claims.put("inicio", inicio);
        claims.put("tope", tope);
        return createToken(claims, actuales.getSubject(), Math.min(ahora + expiration, limite));
    }

    // Inicio de la sesión; los tokens emitidos antes de este cambio no lo traen y valen por su emisión.
    private long inicioDeSesion(Claims claims) {
        Object inicio = claims.get("inicio");
        if (inicio instanceof Number numero) {
            return numero.longValue();
        }
        return claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : System.currentTimeMillis();
    }

    // Crear el token JWT
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, System.currentTimeMillis() + expiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long vencimientoMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(vencimientoMs))
                .signWith(getSigningKey())
                .compact();
    }

    // Validar token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String usuario = extractUsuario(token);
        return (usuario.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
