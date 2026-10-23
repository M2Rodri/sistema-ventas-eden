package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.UsuarioRequest;
import com.mitienda.ecommerce.exception.AccesoProhibidoException;
import com.mitienda.ecommerce.exception.ReglaNegocioException;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El administrador principal ("admin") solo lo puede modificar él mismo; los demás ADMIN reciben 403. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceAdminPrincipalTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RegistroAuditoria registroAuditoria;

    private UsuarioService servicio;
    private Usuario principal;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioRepository, roleRepository, passwordEncoder, registroAuditoria);
        Role admin = new Role(1L, "ADMIN", "Administrador");
        Role empleado = new Role(2L, "EMPLEADO", "Vendedor");
        principal = persona(1L, "admin", admin);
        Usuario otroAdmin = persona(3L, "msoliz", admin);
        ana = persona(7L, "ana", empleado);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(principal));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(ana));
        when(usuarioRepository.findAll()).thenReturn(List.of(principal, otroAdmin, ana));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNombre("ADMIN")).thenReturn(Optional.of(admin));
        when(roleRepository.findByNombre("EMPLEADO")).thenReturn(Optional.of(empleado));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Usuario persona(Long id, String usuario, Role rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsuario(usuario);
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private void entraComo(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private UsuarioRequest edicion(String usuario, String rol, String clave) {
        UsuarioRequest r = new UsuarioRequest();
        r.setNombre("Nombre");
        r.setApellido("Apellido");
        r.setUsuario(usuario);
        r.setRole(rol);
        r.setActivo(true);
        r.setPassword(clave);
        return r;
    }

    private void assertProtegido(Runnable accion) {
        AccesoProhibidoException e = assertThrows(AccesoProhibidoException.class, accion::run);
        assertEquals("ADMIN_PRINCIPAL_PROTEGIDO", e.getCodigo());
        assertEquals(403, e.getEstado().value());
    }

    @Test
    void otroAdminNoPuedeEditarAlPrincipal() {
        entraComo("msoliz");
        assertProtegido(() -> servicio.updateUser(1L, edicion("admin", "ADMIN", "otra-clave-123")));
        assertNull(principal.getSesionesValidasDesde());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void otroAdminNoPuedeDarDeBajaAlPrincipal() {
        entraComo("msoliz");
        assertProtegido(() -> servicio.deleteUser(1L));
        assertTrue(principal.getActivo());
    }

    @Test
    void otroAdminNoPuedeDesactivarAlPrincipal() {
        entraComo("msoliz");
        assertProtegido(() -> servicio.toggleUserStatus(1L));
        assertTrue(principal.getActivo());
    }

    @Test
    void otroAdminNoPuedeCerrarLasSesionesDelPrincipal() {
        entraComo("msoliz");
        assertProtegido(() -> servicio.cerrarSesiones(1L));
        assertNull(principal.getSesionesValidasDesde());
        verify(registroAuditoria, never()).registrar(anyString(), anyString(), any(), anyString());
    }

    @Test
    void sinSesionTampocoSePuedeTocarAlPrincipal() {
        assertProtegido(() -> servicio.cerrarSesiones(1L));
    }

    @Test
    void elPrincipalSiPuedeSobreSiMismo() {
        entraComo("admin");
        servicio.cerrarSesiones(1L);
        assertNotNull(principal.getSesionesValidasDesde());
        servicio.updateUser(1L, edicion("admin", "ADMIN", "nueva-clave-123"));
        assertEquals("hash-nuevo", principal.getPassword());
    }

    @Test
    void elPrincipalNoPuedeCambiarseElNombreDeUsuario() {
        entraComo("admin");
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> servicio.updateUser(1L, edicion("otro.nombre", "ADMIN", null)));
        assertEquals("ADMIN_PRINCIPAL_NO_RENOMBRABLE", e.getCodigo());
        assertEquals("admin", principal.getUsuario());
    }

    @Test
    void losDemasUsuariosSeEditanNormalmenteConCualquierAdmin() {
        entraComo("msoliz");
        servicio.updateUser(7L, edicion("ana", "EMPLEADO", "nueva-clave-123"));
        servicio.toggleUserStatus(7L);
        servicio.cerrarSesiones(7L);
        servicio.deleteUser(7L);
        assertNotNull(ana.getSesionesValidasDesde());
    }
}
