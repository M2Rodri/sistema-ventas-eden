package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuditoriaResponse;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.models.Auditoria;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.AuditoriaRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La auditoría guarda quién hizo qué, sobre qué tabla y desde qué IP, aunque el usuario ya no exista. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditoriaServiceTest {

    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private AuditoriaService servicio;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        servicio = new AuditoriaService(auditoriaRepository, usuarioRepository);
        ana = new Usuario();
        ana.setId(7L);
        ana.setNombre("Ana");
        ana.setApellido("Pérez");
        ana.setUsuario("ana");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(ana));
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(i -> {
            Auditoria a = i.getArgument(0);
            a.setId(1L);
            return a;
        });
    }

    private Auditoria guardada() {
        ArgumentCaptor<Auditoria> captura = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captura.capture());
        return captura.getValue();
    }

    @Test
    void registraElUsuarioLaAccionLaTablaElRegistroYLaIp() {
        AuditoriaResponse r = servicio.registrarAuditoria(7L, "CANCELAR_VENTA", "ventas", "15", "Venta 15", "10.0.0.5");

        Auditoria a = guardada();
        assertEquals(ana, a.getUsuario());
        assertEquals("CANCELAR_VENTA", a.getAccion());
        assertEquals("ventas", a.getTablaAfectada());
        assertEquals("15", a.getIdRegistro());
        assertEquals("Venta 15", a.getDetalles());
        assertEquals("10.0.0.5", a.getIpDispositivo());
        assertEquals(7L, r.getIdUsuario());
    }

    @Test
    void sinUsuarioIdentificadoLaAuditoriaSeGuardaIgual() {
        servicio.registrarAuditoria(null, "LOGIN_FALLIDO", "usuarios", null, "intento", "10.0.0.9");

        assertNull(guardada().getUsuario());
    }

    @Test
    void conUnUsuarioQueYaNoExisteSeGuardaSinUsuario() {
        servicio.registrarAuditoria(999L, "ELIMINAR_PRODUCTO", "productos", "3", null, "10.0.0.5");

        assertNull(guardada().getUsuario());
    }

    @Test
    void lasAccionesComunesArmanSuNombreConLaTablaEnMayuscula() {
        servicio.registrarCreacion(7L, "productos", "3", "10.0.0.5");
        servicio.registrarActualizacion(7L, "clientes", "4", "10.0.0.5");
        servicio.registrarEliminacion(7L, "proveedores", "5", "10.0.0.5");

        ArgumentCaptor<Auditoria> captura = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository, times(3)).save(captura.capture());
        assertEquals("CREAR_PRODUCTOS", captura.getAllValues().get(0).getAccion());
        assertEquals("ACTUALIZAR_CLIENTES", captura.getAllValues().get(1).getAccion());
        assertEquals("ELIMINAR_PROVEEDORES", captura.getAllValues().get(2).getAccion());
    }

    @Test
    void elLoginQuedaRegistradoSobreLaTablaUsuarios() {
        servicio.registrarLogin(7L, "10.0.0.5");

        Auditoria a = guardada();
        assertEquals("LOGIN", a.getAccion());
        assertEquals("usuarios", a.getTablaAfectada());
        assertEquals("7", a.getIdRegistro());
    }

    @Test
    void unaAuditoriaInexistenteEs404ConSuCodigo() {
        when(auditoriaRepository.findById(404L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException e = assertThrows(RecursoNoEncontradoException.class, () -> servicio.getAuditoriaById(404L));

        assertEquals("AUDITORIA_NO_ENCONTRADA", e.getCodigo());
    }
}
