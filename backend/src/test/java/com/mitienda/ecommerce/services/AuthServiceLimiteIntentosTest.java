package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.exception.DemasiadosIntentosException;
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
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El login responde 429 al sexto intento seguido y deja el bloqueo en la auditoría. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceLimiteIntentosTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RegistroAuditoria registroAuditoria;

    private AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarioRepository, jwtUtil, authenticationManager, registroAuditoria,
                new LimiteIntentosLogin());
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("mala"));
    }

    private LoginRequest pedido(String usuario) {
        LoginRequest request = new LoginRequest();
        request.setUsuario(usuario);
        request.setPassword("incorrecta");
        return request;
    }

    @Test
    void alSextoIntentoResponde429SinRevisarLaClave() {
        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> servicio.login(pedido("ana")));
        }

        DemasiadosIntentosException error = assertThrows(DemasiadosIntentosException.class,
                () -> servicio.login(pedido("ana")));

        assertEquals(429, error.getEstado().value());
        assertEquals("DEMASIADOS_INTENTOS", error.getCodigo());
        // Los cinco primeros revisaron la clave; el sexto ni siquiera llegó a revisarla.
        verify(authenticationManager, times(5)).authenticate(any(Authentication.class));
    }

    @Test
    void elBloqueoQuedaEnLaAuditoriaUnaSolaVez() {
        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> servicio.login(pedido("ana")));
        }
        assertThrows(DemasiadosIntentosException.class, () -> servicio.login(pedido("ana")));

        verify(registroAuditoria, times(5)).registrarPara(isNull(), eq("LOGIN_FALLIDO"), eq("usuarios"), isNull(), any());
        verify(registroAuditoria, times(1)).registrarPara(isNull(), eq("LOGIN_BLOQUEADO"), eq("usuarios"), isNull(), any());
    }

    @Test
    void cuatroFallosTodaviaNoBloquean() {
        for (int i = 0; i < 4; i++) {
            assertThrows(BadCredentialsException.class, () -> servicio.login(pedido("ana")));
        }
        verify(registroAuditoria, never()).registrarPara(any(), eq("LOGIN_BLOQUEADO"), any(), any(), any());
        verify(authenticationManager, times(4)).authenticate(any(Authentication.class));
    }

    @Test
    void elBloqueoDeUnUsuarioNoAfectaAOtro() {
        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> servicio.login(pedido("ana")));
        }
        assertThrows(BadCredentialsException.class, () -> servicio.login(pedido("luis")));
    }
}
