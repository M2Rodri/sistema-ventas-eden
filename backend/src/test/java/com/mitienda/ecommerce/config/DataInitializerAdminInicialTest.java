package com.mitienda.ecommerce.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.services.RegistroAuditoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Al arrancar con la tabla usuarios vacía y las variables definidas se crea el administrador inicial. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerAdminInicialTest {

    private static final String CLAVE = "Clave-Inicial-Segura-9";

    @Mock private RoleRepository roleRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RegistroAuditoria registroAuditoria;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Role rolAdmin = new Role(1L, "ADMIN", "Administrador");
    private final Role rolEmpleado = new Role(2L, "EMPLEADO", "Vendedor");

    private Logger logDelInicializador;
    private ListAppender<ILoggingEvent> registro;

    @BeforeEach
    void preparar() {
        when(roleRepository.findByNombre("ADMIN")).thenReturn(Optional.of(rolAdmin));
        when(roleRepository.findByNombre("EMPLEADO")).thenReturn(Optional.of(rolEmpleado));
        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        logDelInicializador = (Logger) LoggerFactory.getLogger(DataInitializer.class);
        logDelInicializador.setLevel(Level.DEBUG);
        registro = new ListAppender<>();
        registro.start();
        logDelInicializador.addAppender(registro);
    }

    @AfterEach
    void limpiar() {
        logDelInicializador.detachAppender(registro);
    }

    private DataInitializer inicializador(String usuario, String clave) {
        return new DataInitializer(roleRepository, usuarioRepository, passwordEncoder, registroAuditoria, usuario, clave);
    }

    @Test
    void conVariablesYSinUsuariosCreaUnAdministradorActivo() {
        inicializador("Dueno.Eden", CLAVE).run();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        Usuario admin = guardado.getValue();
        assertEquals("dueno.eden", admin.getUsuario(), "el usuario se guarda en minúsculas, como en el login");
        assertEquals("ADMIN", admin.getRoleName());
        assertTrue(admin.getActivo());
    }

    @Test
    void laContrasenaSeGuardaConBCryptYNuncaEnTextoPlano() {
        inicializador("dueno", CLAVE).run();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        String guardada = guardado.getValue().getPassword();
        assertNotEquals(CLAVE, guardada);
        assertTrue(guardada.startsWith("$2"), "debe ser un hash BCrypt");
        assertTrue(passwordEncoder.matches(CLAVE, guardada), "el hash corresponde a la clave del entorno");
    }

    @Test
    void laContrasenaNuncaApareceEnElRegistro() {
        inicializador("dueno", CLAVE).run();                // camino feliz
        inicializador("dueno", "abc").run();                // clave inválida: error
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("falló con " + CLAVE));
        inicializador("dueno", CLAVE).run();                // error al guardar con la clave en el mensaje

        assertFalse(registro.list.isEmpty(), "debe haber dejado mensajes de registro");
        for (ILoggingEvent evento : registro.list) {
            assertFalse(evento.getFormattedMessage().contains(CLAVE), evento.getFormattedMessage());
            assertFalse(evento.getFormattedMessage().contains("abc"), evento.getFormattedMessage());
            assertEquals(null, evento.getThrowableProxy(), "no se registra la excepción, que podría traer datos");
        }
        verify(registroAuditoria).registrarPara(any(), anyString(), anyString(), any(), org.mockito.ArgumentMatchers.argThat(
                detalle -> detalle != null && !detalle.contains(CLAVE)));
    }

    @Test
    void sinLasVariablesNoHaceNada() {
        inicializador("", "").run();
        inicializador("dueno", "").run();
        inicializador("", CLAVE).run();
        inicializador(null, null).run();

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).count();
    }

    @Test
    void siYaHayUsuariosNoCreaNiModificaNada() {
        when(usuarioRepository.count()).thenReturn(3L);

        inicializador("dueno", CLAVE).run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void conUnUsuarioOUnaClaveInvalidosNoCreaNadaYSigueArrancando() {
        inicializador("ab", CLAVE).run();              // usuario demasiado corto
        inicializador("con espacio", CLAVE).run();     // caracteres no permitidos
        inicializador("dueno", "12345").run();         // clave de menos de 6

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void siFallaAlGuardarNoCortaElArranque() {
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("base caída"));

        inicializador("dueno", CLAVE).run(); // no debe lanzar
    }

    @Test
    void dejaElAltaEnLaAuditoria() {
        inicializador("dueno", CLAVE).run();

        verify(registroAuditoria).registrarPara(any(), anyString(), anyString(), any(), anyString());
    }
}
