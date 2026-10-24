package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.exception.AccesoProhibidoException;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.security.JwtUtil;
import com.mitienda.ecommerce.security.LimiteIntentosLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La app móvil es solo para administradores: el Empleado no inicia sesión ni renueva en ella; la web no cambia. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceAppMovilTest {
    private static final String CLAVE = "clave-de-prueba-para-firmar-tokens-de-al-menos-64-caracteres-0123456789";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RegistroAuditoria registroAuditoria;

    private JwtUtil jwtUtil;
    private AuthService servicio;

    @BeforeEach
    void preparar() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", CLAVE);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 4L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(jwtUtil, "sesionMaxima", 8L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(jwtUtil, "sesionMaximaMovil", 12L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(jwtUtil, "sesionMaximaMovilSinCandado", 4L * 60 * 60 * 1000);
        servicio = new AuthService(usuarioRepository, jwtUtil, authenticationManager, registroAuditoria,
                new LimiteIntentosLogin());
    }

    private Usuario usuario(String nombre, String rol) {
        Role role = new Role();
        role.setNombre(rol);
        Usuario usuario = new Usuario("Ana", "Pérez", nombre, "hash", role);
        usuario.setId(7L);
        when(usuarioRepository.findByUsuario(nombre)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private LoginRequest pedido(String usuario, String perfil) {
        LoginRequest request = new LoginRequest();
        request.setUsuario(usuario);
        request.setPassword("clave-correcta");
        request.setPerfilSesion(perfil);
        return request;
    }

    @Test
    void unEmpleadoNoIniciaSesionEnLaAppConCandado() {
        usuario("luis", "EMPLEADO");

        AccesoProhibidoException error = assertThrows(AccesoProhibidoException.class,
                () -> servicio.login(pedido("luis", JwtUtil.PERFIL_MOVIL_CON_CANDADO)));

        assertEquals(403, error.getEstado().value());
        assertEquals("APP_SOLO_ADMIN", error.getCodigo());
    }

    @Test
    void unEmpleadoNoIniciaSesionEnLaAppSinCandado() {
        usuario("luis", "EMPLEADO");

        assertThrows(AccesoProhibidoException.class,
                () -> servicio.login(pedido("luis", JwtUtil.PERFIL_MOVIL_SIN_CANDADO)));
    }

    @Test
    void elRechazoQuedaEnLaAuditoriaYNoSeEmiteToken() {
        usuario("luis", "EMPLEADO");

        assertThrows(AccesoProhibidoException.class,
                () -> servicio.login(pedido("luis", JwtUtil.PERFIL_MOVIL_CON_CANDADO)));

        verify(registroAuditoria).registrarPara(eq(7L), eq("LOGIN_RECHAZADO"), eq("usuarios"), eq(7L), any());
        verify(registroAuditoria, never()).registrarPara(any(), eq("LOGIN"), any(), any(), any());
    }

    @Test
    void unAdministradorSiIniciaSesionEnLaApp() {
        usuario("ana", "ADMIN");

        AuthResponse respuesta = servicio.login(pedido("ana", JwtUtil.PERFIL_MOVIL_CON_CANDADO));

        assertNotNull(respuesta.getToken());
        assertTrue(jwtUtil.esSesionMovil(respuesta.getToken()));
    }

    @Test
    void unEmpleadoSiIniciaSesionEnLaWeb() {
        usuario("luis", "EMPLEADO");

        AuthResponse respuesta = servicio.login(pedido("luis", null));

        assertNotNull(respuesta.getToken());
        assertFalse(jwtUtil.esSesionMovil(respuesta.getToken()));
    }

    @Test
    void laRenovacionConservaLaMarcaDeLaApp() {
        usuario("ana", "ADMIN");
        String token = servicio.login(pedido("ana", JwtUtil.PERFIL_MOVIL_SIN_CANDADO)).getToken();

        AuthResponse renovado = servicio.renovar("Bearer " + token);

        assertTrue(jwtUtil.esSesionMovil(renovado.getToken()));
    }

    @Test
    void unAdministradorQueDejoDeSerloNoRenuevaLaSesionDeLaApp() {
        Usuario ana = usuario("ana", "ADMIN");
        String token = servicio.login(pedido("ana", JwtUtil.PERFIL_MOVIL_CON_CANDADO)).getToken();
        Role empleado = new Role();
        empleado.setNombre("EMPLEADO");
        ana.setRol(empleado);

        assertThrows(BadCredentialsException.class, () -> servicio.renovar("Bearer " + token));
    }

    @Test
    void laRenovacionDeLaWebDeUnEmpleadoSigueFuncionando() {
        usuario("luis", "EMPLEADO");
        String token = servicio.login(pedido("luis", null)).getToken();

        assertNotNull(servicio.renovar("Bearer " + token).getToken());
    }
}
