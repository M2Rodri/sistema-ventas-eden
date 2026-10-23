package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.security.JwtUtil;
import com.mitienda.ecommerce.security.LimiteIntentosLogin;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** POST /auth/renovar: solo renueva sesiones vivas de usuarios activos y toma el rol de la base. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceRenovarTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RegistroAuditoria registroAuditoria;

    private AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarioRepository, jwtUtil, authenticationManager, registroAuditoria,
                new LimiteIntentosLogin());
    }

    private Usuario usuario(String nombre, boolean activo, String rol) {
        Role role = new Role();
        role.setNombre(rol);
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsuario(nombre);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setActivo(activo);
        usuario.setRol(role);
        return usuario;
    }

    @Test
    void renuevaConElRolDeLaBaseNoElDelTokenViejo() {
        when(jwtUtil.extractUsuario("viejo")).thenReturn("ana");
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(usuario("ana", true, "EMPLEADO")));
        when(jwtUtil.renovarToken("viejo", "EMPLEADO")).thenReturn("nuevo");

        AuthResponse respuesta = servicio.renovar("Bearer viejo");

        assertEquals("nuevo", respuesta.getToken());
        assertEquals("EMPLEADO", respuesta.getRole());
        assertEquals("ana", respuesta.getUsuario());
    }

    @Test
    void sinCabeceraAuthorizationResponde401() {
        assertThrows(BadCredentialsException.class, () -> servicio.renovar(null));
        assertThrows(BadCredentialsException.class, () -> servicio.renovar("Basic abc"));
        verify(jwtUtil, never()).renovarToken(anyString(), anyString());
    }

    @Test
    void unTokenInvalidoOVencidoResponde401() {
        when(jwtUtil.extractUsuario("malo")).thenThrow(new ExpiredJwtException(null, null, "vencido"));

        assertThrows(BadCredentialsException.class, () -> servicio.renovar("Bearer malo"));
    }

    @Test
    void unUsuarioDadoDeBajaNoRenueva() {
        when(jwtUtil.extractUsuario("viejo")).thenReturn("ana");
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(usuario("ana", false, "EMPLEADO")));

        assertThrows(DisabledException.class, () -> servicio.renovar("Bearer viejo"));
        verify(jwtUtil, never()).renovarToken(anyString(), anyString());
    }

    @Test
    void unUsuarioInexistenteResponde401() {
        when(jwtUtil.extractUsuario("viejo")).thenReturn("fantasma");
        when(usuarioRepository.findByUsuario("fantasma")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> servicio.renovar("Bearer viejo"));
    }

    // ---- corte de sesiones ----

    @Test
    void unaSesionCortadaNoSeRenueva() {
        Usuario ana = usuario("ana", true, "EMPLEADO");
        ana.setSesionesValidasDesde(2_000L);
        when(jwtUtil.extractUsuario("viejo")).thenReturn("ana");
        when(jwtUtil.extractInicioSesion("viejo")).thenReturn(1_000L); // la sesión empezó antes del corte
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(ana));

        assertThrows(BadCredentialsException.class, () -> servicio.renovar("Bearer viejo"));
        verify(jwtUtil, never()).renovarToken(anyString(), anyString());
    }

    @Test
    void unaSesionIniciadaDespuesDelCorteSiSeRenueva() {
        Usuario ana = usuario("ana", true, "EMPLEADO");
        ana.setSesionesValidasDesde(2_000L);
        when(jwtUtil.extractUsuario("nuevo-login")).thenReturn("ana");
        when(jwtUtil.extractInicioSesion("nuevo-login")).thenReturn(3_000L);
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(ana));
        when(jwtUtil.renovarToken("nuevo-login", "EMPLEADO")).thenReturn("renovado");

        assertEquals("renovado", servicio.renovar("Bearer nuevo-login").getToken());
    }

    @Test
    void cerrarSesionesFijaElCorteYLoDejaEnLaAuditoria() {
        Usuario ana = usuario("ana", true, "EMPLEADO");
        when(jwtUtil.extractUsuario("vigente")).thenReturn("ana");
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(ana));
        long antes = System.currentTimeMillis();

        servicio.cerrarSesiones("Bearer vigente");

        assertNotNull(ana.getSesionesValidasDesde());
        assertTrue(ana.getSesionesValidasDesde() >= antes, "el corte debe ser de ahora");
        verify(usuarioRepository).save(ana);
        verify(registroAuditoria).registrarPara(eq(7L), eq("CERRAR_SESIONES"), eq("usuarios"), eq(7L), anyString());
    }

    @Test
    void cerrarSesionesSinTokenValidoResponde401YNoCortaNada() {
        assertThrows(BadCredentialsException.class, () -> servicio.cerrarSesiones(null));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---- topes por tipo de cliente ----

    @Test
    void elLoginPasaElPerfilDeSesionAlToken() {
        Usuario ana = usuario("ana", true, "EMPLEADO");
        when(usuarioRepository.findByUsuario("ana")).thenReturn(Optional.of(ana));
        when(jwtUtil.generateToken("ana", "EMPLEADO", JwtUtil.PERFIL_MOVIL_CON_CANDADO)).thenReturn("token-movil");
        LoginRequest pedido = new LoginRequest();
        pedido.setUsuario("ana");
        pedido.setPassword("clave");
        pedido.setPerfilSesion(JwtUtil.PERFIL_MOVIL_CON_CANDADO);

        AuthResponse respuesta = servicio.login(pedido);

        assertEquals("token-movil", respuesta.getToken());
    }
}
