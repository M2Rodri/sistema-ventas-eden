package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.UsuarioRequest;
import com.mitienda.ecommerce.dto.UsuarioResponse;
import com.mitienda.ecommerce.exception.PeticionInvalidaException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Al crear un usuario sin rol queda como EMPLEADO; un rol que no existe sigue siendo error. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceRolPorDefectoTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RegistroAuditoria registroAuditoria;

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioRepository, roleRepository, passwordEncoder, registroAuditoria);
        when(usuarioRepository.existsByUsuario(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(roleRepository.findByNombre("EMPLEADO")).thenReturn(Optional.of(new Role(2L, "EMPLEADO", "Vendedor")));
        when(roleRepository.findByNombre("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN", "Dueño")));
    }

    private UsuarioRequest pedido(String rol) {
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Ana");
        request.setApellido("Perez");
        request.setUsuario("ana.perez");
        request.setPassword("secreta1");
        request.setRole(rol);
        request.setActivo(true);
        return request;
    }

    @Test
    void sinRolQuedaComoEmpleado() {
        UsuarioResponse nuevo = servicio.createUser(pedido(null));
        assertEquals("EMPLEADO", nuevo.getRole());
    }

    @Test
    void conRolEnBlancoQuedaComoEmpleado() {
        assertEquals("EMPLEADO", servicio.createUser(pedido("  ")).getRole());
    }

    @Test
    void conRolAdminSeRespeta() {
        assertEquals("ADMIN", servicio.createUser(pedido("ADMIN")).getRole());
    }

    @Test
    void unRolQueNoExisteSigueSiendoError() {
        when(roleRepository.findByNombre("DUENO")).thenReturn(Optional.empty());
        assertThrows(PeticionInvalidaException.class, () -> servicio.createUser(pedido("DUENO")));
    }
}
