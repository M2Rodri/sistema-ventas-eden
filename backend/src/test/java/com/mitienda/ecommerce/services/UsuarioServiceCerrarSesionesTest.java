package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.UsuarioRequest;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cortar las sesiones de un usuario: con el botón de ADMIN y al cambiarle la contraseña. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceCerrarSesionesTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RegistroAuditoria registroAuditoria;

    private UsuarioService servicio;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioRepository, roleRepository, passwordEncoder, registroAuditoria);
        Role empleado = new Role(2L, "EMPLEADO", "Vendedor");
        ana = new Usuario();
        ana.setId(7L);
        ana.setUsuario("ana");
        ana.setNombre("Ana");
        ana.setApellido("Pérez");
        ana.setRol(empleado);
        ana.setActivo(true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(ana));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNombre("EMPLEADO")).thenReturn(Optional.of(empleado));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");
    }

    private UsuarioRequest edicion(String clave) {
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Ana");
        request.setApellido("Pérez");
        request.setUsuario("ana");
        request.setRole("EMPLEADO");
        request.setActivo(true);
        request.setPassword(clave);
        return request;
    }

    @Test
    void cerrarSesionesFijaElCorteYLoAudita() {
        long antes = System.currentTimeMillis();

        servicio.cerrarSesiones(7L);

        assertNotNull(ana.getSesionesValidasDesde());
        assertTrue(ana.getSesionesValidasDesde() >= antes);
        verify(usuarioRepository).save(ana);
        verify(registroAuditoria).registrar(org.mockito.ArgumentMatchers.eq("CERRAR_SESIONES"),
                org.mockito.ArgumentMatchers.eq("usuarios"), org.mockito.ArgumentMatchers.eq(7L), anyString());
    }

    @Test
    void cerrarSesionesDeUnUsuarioQueNoExisteEsError() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cerrarSesiones(99L));
    }

    @Test
    void cambiarLaContrasenaCortaLasSesionesAbiertas() {
        long antes = System.currentTimeMillis();

        servicio.updateUser(7L, edicion("clave-nueva-123"));

        assertNotNull(ana.getSesionesValidasDesde());
        assertTrue(ana.getSesionesValidasDesde() >= antes);
    }

    @Test
    void editarSinCambiarLaContrasenaNoCortaNada() {
        servicio.updateUser(7L, edicion(null));

        assertNull(ana.getSesionesValidasDesde());
    }

    @Test
    void unaContrasenaEnBlancoTampocoCorta() {
        servicio.updateUser(7L, edicion("   "));

        assertNull(ana.getSesionesValidasDesde());
    }
}
