package com.mitienda.ecommerce.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renovación del token: conserva el usuario y el inicio de sesión, pone el rol vigente y respeta el tope. */
class JwtUtilRenovacionTest {
    private static final String CLAVE = "clave-de-prueba-para-firmar-tokens-de-al-menos-64-caracteres-0123456789";
    private static final long HORA = 60L * 60 * 1000;
    private static final long CUATRO_HORAS = 4 * HORA;
    private static final long OCHO_HORAS = 8 * HORA;
    private static final long DOCE_HORAS = 12 * HORA;

    private JwtUtil jwt(String clave, long expiracion, long sesionMaxima) {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", clave);
        ReflectionTestUtils.setField(jwt, "expiration", expiracion);
        ReflectionTestUtils.setField(jwt, "sesionMaxima", sesionMaxima);
        ReflectionTestUtils.setField(jwt, "sesionMaximaMovil", DOCE_HORAS);
        ReflectionTestUtils.setField(jwt, "sesionMaximaMovilSinCandado", CUATRO_HORAS);
        return jwt;
    }

    private long claimLargo(JwtUtil jwt, String token, String nombre) {
        return jwt.extractClaim(token, claims -> ((Number) claims.get(nombre)).longValue());
    }

    @Test
    void renovarConservaElUsuarioYPoneElRolVigente() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);
        String viejo = jwt.generateToken("ana", "ADMIN");

        String nuevo = jwt.renovarToken(viejo, "EMPLEADO");

        assertEquals("ana", jwt.extractUsuario(nuevo));
        assertEquals("EMPLEADO", jwt.extractRole(nuevo));
    }

    @Test
    void renovarConservaElInicioDeLaSesion() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);
        String viejo = jwt.generateToken("ana", "ADMIN");

        String nuevo = jwt.renovarToken(viejo, "ADMIN");

        assertEquals(claimLargo(jwt, viejo, "inicio"), claimLargo(jwt, nuevo, "inicio"));
        assertEquals(jwt.extractInicioSesion(viejo), jwt.extractInicioSesion(nuevo));
    }

    @Test
    void elVencimientoNuevoNuncaPasaDelTopeDeLaSesion() {
        // Token de 4 horas pero sesión máxima de 1 hora: la renovación no puede pasar de esa hora.
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, HORA);
        String viejo = jwt.generateToken("ana", "ADMIN");
        long inicio = claimLargo(jwt, viejo, "inicio");

        String nuevo = jwt.renovarToken(viejo, "ADMIN");

        Date vence = jwt.extractExpiration(nuevo);
        assertTrue(vence.getTime() <= inicio + HORA, "el token renovado no puede vencer después del tope");
    }

    @Test
    void sinRenovarElTokenDuraLoQueDiceLaConfiguracion() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);
        long antes = System.currentTimeMillis();

        String token = jwt.generateToken("ana", "ADMIN");

        long duracion = jwt.extractExpiration(token).getTime() - antes;
        assertTrue(duracion > CUATRO_HORAS - 5_000 && duracion <= CUATRO_HORAS + 5_000,
                "debería durar unas 4 horas, duró " + duracion + " ms");
    }

    @Test
    void siLaSesionLlegoAlTopeNoSeRenueva() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, 0L);
        String viejo = jwt.generateToken("ana", "ADMIN");

        assertThrows(CredentialsExpiredException.class, () -> jwt.renovarToken(viejo, "ADMIN"));
    }

    @Test
    void unTokenVencidoNoSeRenueva() {
        JwtUtil jwt = jwt(CLAVE, -1_000L, OCHO_HORAS);
        String vencido = jwt.generateToken("ana", "ADMIN");

        assertThrows(JwtException.class, () -> jwt.renovarToken(vencido, "ADMIN"));
    }

    @Test
    void unTokenFirmadoConOtraClaveNoSeRenueva() {
        JwtUtil ajeno = jwt(CLAVE + "-distinta-0000000000000000", CUATRO_HORAS, OCHO_HORAS);
        String falso = ajeno.generateToken("ana", "ADMIN");

        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);

        assertThrows(JwtException.class, () -> jwt.renovarToken(falso, "ADMIN"));
    }

    @Test
    void unTokenEmitidoAntesDelCambioSinInicioSeRenuevaUsandoSuEmision() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);
        // generateToken(usuario) a secas no trae "inicio": es como los tokens de antes de este cambio.
        String antiguo = jwt.generateToken("ana");

        String nuevo = jwt.renovarToken(antiguo, "EMPLEADO");

        assertEquals("ana", jwt.extractUsuario(nuevo));
        assertEquals("EMPLEADO", jwt.extractRole(nuevo));
    }

    // ---- topes según desde dónde se inicia sesión ----

    @Test
    void laWebTieneElTopeDeOchoHoras() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);

        assertEquals(OCHO_HORAS, claimLargo(jwt, jwt.generateToken("ana", "ADMIN", null), "tope"));
        assertEquals(OCHO_HORAS, claimLargo(jwt, jwt.generateToken("ana", "ADMIN", "WEB"), "tope"));
        assertEquals(OCHO_HORAS, claimLargo(jwt, jwt.generateToken("ana", "ADMIN", "cualquier-cosa"), "tope"));
    }

    @Test
    void laAppConCandadoTieneElTopeDeDoceHoras() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);

        String token = jwt.generateToken("ana", "ADMIN", JwtUtil.PERFIL_MOVIL_CON_CANDADO);

        assertEquals(DOCE_HORAS, claimLargo(jwt, token, "tope"));
    }

    @Test
    void laAppSinCandadoTieneElTopeDeCuatroHoras() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);

        String token = jwt.generateToken("ana", "ADMIN", JwtUtil.PERFIL_MOVIL_SIN_CANDADO);

        assertEquals(CUATRO_HORAS, claimLargo(jwt, token, "tope"));
    }

    @Test
    void laRenovacionConservaElTopeDelPerfil() {
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);
        String conCandado = jwt.generateToken("ana", "ADMIN", JwtUtil.PERFIL_MOVIL_CON_CANDADO);

        String renovado = jwt.renovarToken(conCandado, "ADMIN");

        assertEquals(DOCE_HORAS, claimLargo(jwt, renovado, "tope"));
    }

    @Test
    void unaSesionDeAppSinCandadoNoSeRenuevaPasadasSusCuatroHoras() {
        // Con un token de 4 h y un tope de 4 h, la primera renovación ya no puede alargar nada.
        JwtUtil jwt = jwt(CLAVE, CUATRO_HORAS, OCHO_HORAS);
        ReflectionTestUtils.setField(jwt, "sesionMaximaMovilSinCandado", 0L);
        String token = jwt.generateToken("ana", "ADMIN", JwtUtil.PERFIL_MOVIL_SIN_CANDADO);

        assertThrows(CredentialsExpiredException.class, () -> jwt.renovarToken(token, "ADMIN"));
    }
}
