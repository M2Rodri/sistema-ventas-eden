package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ProveedorRequest;
import com.mitienda.ecommerce.dto.ProveedorResponse;
import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.models.Proveedor;
import com.mitienda.ecommerce.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de proveedores: NIT único, alta, edición y baja por activar/desactivar. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProveedorServiceTest {

    @Mock private ProveedorRepository proveedorRepository;
    @Mock private RegistroAuditoria registroAuditoria;

    private ProveedorService servicio;
    private Proveedor existente;

    @BeforeEach
    void preparar() {
        servicio = new ProveedorService(proveedorRepository, registroAuditoria);
        existente = new Proveedor();
        existente.setId(1L);
        existente.setNombreEmpresa("Colchones del Oriente");
        existente.setNit("1020304050");
        existente.setActivo(true);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(i -> {
            Proveedor p = i.getArgument(0);
            if (p.getId() == null) p.setId(50L);
            return p;
        });
    }

    private ProveedorRequest peticion(String empresa, String nit) {
        ProveedorRequest r = new ProveedorRequest();
        r.setNombreEmpresa(empresa);
        r.setNit(nit);
        r.setContacto("Juan Pérez");
        r.setTelefono("70000000");
        r.setActivo(true);
        return r;
    }

    @Test
    void crearConUnNitNuevoGuardaYAudita() {
        ProveedorResponse r = servicio.createProveedor(peticion("Maderas del Sur", "999"));

        assertEquals(50L, r.getId());
        verify(registroAuditoria).registrar(eq("CREAR_PROVEEDOR"), eq("proveedores"), eq(50L), anyString());
    }

    @Test
    void crearConUnNitYaUsadoEs409() {
        when(proveedorRepository.existsByNit("1020304050")).thenReturn(true);

        ConflictoEstadoException e = assertThrows(ConflictoEstadoException.class,
                () -> servicio.createProveedor(peticion("Otra empresa", "1020304050")));

        assertEquals("NIT_DUPLICADO", e.getCodigo());
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void editarSinCambiarElNitNoEsConflictoAunqueElNitYaExista() {
        when(proveedorRepository.existsByNit("1020304050")).thenReturn(true);

        ProveedorResponse r = servicio.updateProveedor(1L, peticion("Colchones del Oriente S.R.L.", "1020304050"));

        assertEquals("Colchones del Oriente S.R.L.", r.getNombreEmpresa());
    }

    @Test
    void editarConElNitDeOtroProveedorEs409() {
        when(proveedorRepository.existsByNit("555")).thenReturn(true);

        ConflictoEstadoException e = assertThrows(ConflictoEstadoException.class,
                () -> servicio.updateProveedor(1L, peticion("Colchones del Oriente", "555")));

        assertEquals("NIT_DUPLICADO", e.getCodigo());
        assertEquals("1020304050", existente.getNit());
    }

    @Test
    void unProveedorInexistenteEs404() {
        RecursoNoEncontradoException e = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.getProveedorById(404L));
        assertEquals("PROVEEDOR_NO_ENCONTRADO", e.getCodigo());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.updateProveedor(404L, peticion("X", "1")));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.toggleProveedorStatus(404L));
    }

    @Test
    void activarYDesactivarAlternaElEstadoYAudita() {
        ProveedorResponse inactivo = servicio.toggleProveedorStatus(1L);
        assertFalse(inactivo.getActivo());
        verify(registroAuditoria).registrar(eq("DESACTIVAR_PROVEEDOR"), eq("proveedores"), eq(1L), anyString());

        ProveedorResponse activo = servicio.toggleProveedorStatus(1L);
        assertTrue(activo.getActivo());
        verify(registroAuditoria).registrar(eq("ACTIVAR_PROVEEDOR"), eq("proveedores"), eq(1L), anyString());
    }
}
