package com.mitienda.ecommerce.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El filtro JWT solo autentica si el token es auténtico y vigente, la cuenta sigue activa y la sesión
 * no fue cortada; y el rol sale de la base, no del token.
 */
class JwtAuthFilterTest {

    private static final String CLAVE = "clave-de-prueba-para-firmar-tokens-de-al-menos-64-caracteres-0123456789";

    private JwtUtil jwtUtil;
    private UserDetailsServiceImpl detalles;
    private JwtAuthFilter filtro;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", CLAVE);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 4L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(jwtUtil, "sesionMaxima", 8L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(jwtUtil, "sesionMaximaMovil", 12L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(jwtUtil, "sesionMaximaMovilSinCandado", 4L * 60 * 60 * 1000);
        detalles = mock(UserDetailsServiceImpl.class);
        filtro = new JwtAuthFilter(jwtUtil, detalles);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private DetalleUsuario usuario(boolean activo, String rol, Long corte) {
        return new DetalleUsuario("ana", "hash", activo, List.of(new SimpleGrantedAuthority("ROLE_" + rol)), corte);
    }

    private void pasarPeticionConToken(String token) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("Authorization", "Bearer " + token);
        filtro.doFilter(peticion, new MockHttpServletResponse(), mock(FilterChain.class));
    }

    @Test
    void conTokenValidoYCuentaActivaAutentica() throws Exception {
        when(detalles.loadUserByUsername(anyString())).thenReturn(usuario(true, "ADMIN", null));

        pasarPeticionConToken(jwtUtil.generateToken("ana", "ADMIN"));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unUsuarioDadoDeBajaNoAutenticaAunqueSuTokenSigaVigente() throws Exception {
        when(detalles.loadUserByUsername(anyString())).thenReturn(usuario(false, "ADMIN", null));

        pasarPeticionConToken(jwtUtil.generateToken("ana", "ADMIN"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unaSesionCortadaDespuesDeSuInicioNoAutentica() throws Exception {
        String token = jwtUtil.generateToken("ana", "ADMIN");
        long corte = System.currentTimeMillis() + 60_000;
        when(detalles.loadUserByUsername(anyString())).thenReturn(usuario(true, "ADMIN", corte));

        pasarPeticionConToken(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unaSesionIniciadaDespuesDelCorteSiAutentica() throws Exception {
        long corte = System.currentTimeMillis() - 60_000;
        when(detalles.loadUserByUsername(anyString())).thenReturn(usuario(true, "ADMIN", corte));

        pasarPeticionConToken(jwtUtil.generateToken("ana", "ADMIN"));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void elRolSalDeLaBaseNoDelToken() throws Exception {
        // El token dice ADMIN, pero la base ya la tiene como EMPLEADO.
        String tokenViejo = jwtUtil.generateToken("ana", "ADMIN");
        when(detalles.loadUserByUsername(anyString())).thenReturn(usuario(true, "EMPLEADO", null));

        pasarPeticionConToken(tokenViejo);

        var autoridades = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assertEquals(1, autoridades.size());
        assertEquals("ROLE_EMPLEADO", autoridades.iterator().next().getAuthority());
    }

    @Test
    void sinCabeceraNoAutentica() throws Exception {
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
