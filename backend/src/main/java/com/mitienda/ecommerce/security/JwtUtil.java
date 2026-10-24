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

/** Utilidad para generar y validar tokens JWT */
@Component
public class JwtUtil {
    /** Perfil de sesión de la app móvil cuyo celular tiene un candado (huella o bloqueo de pantalla). */
    public static final String PERFIL_MOVIL_CON_CANDADO = "MOVIL_CON_CANDADO";
    /** Perfil de sesión de la app móvil cuyo celular no tiene ningún candado. */
    public static final String PERFIL_MOVIL_SIN_CANDADO = "MOVIL_SIN_CANDADO";
    /** Marca que llevan los tokens de la app móvil; las renovaciones la conservan. */
    private static final String MARCA_MOVIL = "MOVIL";

    @Value("${jwt.secret}")
    private String secret;

    /** Vida de cada token. */
    @Value("${jwt.expiration}")
    private Long expiration;

    /** Tope de sesión (el de la web) aunque se renueve el token: sin él, un token robado no vencería nunca. */
    @Value("${jwt.sesion-maxima:28800000}")
    private Long sesionMaxima;

    /** Tope para la app móvil cuyo celular tiene candado: la huella protege la sesión, puede ser más larga. */
    @Value("${jwt.sesion-maxima-movil:43200000}")
    private Long sesionMaximaMovil;

    /** Tope para la app móvil cuyo celular no tiene candado: sin protección extra, más corto. */
    @Value("${jwt.sesion-maxima-movil-sin-candado:14400000}")
    private Long sesionMaximaMovilSinCandado;

    /** true si el perfil de sesión es el de la app móvil (con o sin candado). */
    public static boolean esPerfilMovil(String perfilSesion) {
        return PERFIL_MOVIL_CON_CANDADO.equals(perfilSesion) || PERFIL_MOVIL_SIN_CANDADO.equals(perfilSesion);
    }

    /** true si el token se emitió para la app móvil, que es solo para administradores. */
    public boolean esSesionMovil(String token) {
        return MARCA_MOVIL.equals(extractClaim(token, claims -> claims.get("app", String.class)));
    }

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

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Generar token para un usuario (SOBRECARGA - CON ROL), sesión de la web
    public String generateToken(String usuario, String role) {
        return generateToken(usuario, role, null);
    }

    /** Genera el token de un inicio de sesión; el perfil (web o app) fija el tope, y null o desconocido es la web. */
    public String generateToken(String usuario, String role, String perfilSesion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        // Inicio de la sesión: las renovaciones lo conservan y con él se calculan el tope y el corte de sesiones.
        claims.put("inicio", System.currentTimeMillis());
        // Tope de esta sesión según el perfil; las renovaciones lo conservan.
        claims.put("tope", topeDelPerfil(perfilSesion));
        if (esPerfilMovil(perfilSesion)) {
            claims.put("app", MARCA_MOVIL);
        }
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

    /** Renueva un token válido sin pasar del tope de la sesión; si venció o llegó al tope, hay que iniciar sesión de nuevo. */
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
        if (MARCA_MOVIL.equals(actuales.get("app", String.class))) {
            claims.put("app", MARCA_MOVIL);
        }
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

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String usuario = extractUsuario(token);
        return (usuario.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
