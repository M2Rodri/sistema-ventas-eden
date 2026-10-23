package com.mitienda.ecommerce.security;

import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.services.RegistroAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El administrador principal ("admin") con la aplicación completa: otro ADMIN recibe 403 al editarlo,
 * darlo de baja, desactivarlo o cerrarle las sesiones; él sí puede sobre sí mismo, y los demás usuarios
 * se editan con normalidad. Si "admin" no existe en la base local, la prueba lo crea (y se revierte).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPrincipalIntegracionTest {

    private static final RequestPostProcessor OTRO_ADMIN = user("it.otro.admin").roles("ADMIN");
    private static final RequestPostProcessor EL_PRINCIPAL = user("admin").roles("ADMIN");
    private static final String EDICION = """
            {"nombre":"Cambiado","apellido":"Cambiado","usuario":"%s","role":"ADMIN","activo":true,"password":"clave-nueva-123"}""";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder codificador;

    /** La auditoría usa su propia transacción y no se revierte: se reemplaza para no ensuciar la base. */
    @MockitoBean private RegistroAuditoria registroAuditoria;

    private Usuario principal;
    private Usuario empleado;

    @BeforeEach
    void preparar() {
        Role admin = roles.findByNombre("ADMIN").orElseThrow();
        principal = usuarios.findByUsuario("admin")
                .orElseGet(() -> usuarios.saveAndFlush(new Usuario("Admin", "Principal", "admin", codificador.encode("x-123456"), admin)));
        usuarios.saveAndFlush(new Usuario("Otro", "Admin", "it.otro.admin", codificador.encode("x-123456"), admin));
        empleado = usuarios.saveAndFlush(new Usuario("Emp", "Prueba", "it.emp.principal", codificador.encode("x-123456"),
                roles.findByNombre("EMPLEADO").orElseThrow()));
    }

    @Test
    void otroAdminRecibe403EnLasCuatroAccionesSobreElPrincipal() throws Exception {
        Long id = principal.getId();
        String hashAntes = principal.getPassword();

        mvc.perform(put("/api/v1/usuarios/" + id).with(OTRO_ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(EDICION.formatted("admin")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.codigo").value("ADMIN_PRINCIPAL_PROTEGIDO"));
        mvc.perform(patch("/api/v1/usuarios/" + id + "/toggle-status").with(OTRO_ADMIN)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/usuarios/" + id).with(OTRO_ADMIN)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/usuarios/" + id + "/cerrar-sesiones").with(OTRO_ADMIN)).andExpect(status().isForbidden());

        usuarios.flush();
        Usuario despues = usuarios.findById(id).orElseThrow();
        assertEquals(hashAntes, despues.getPassword());
        assertTrue(despues.getActivo());
        assertNull(despues.getSesionesValidasDesde());
    }

    @Test
    void elPrincipalPuedeCerrarSusPropiasSesiones() throws Exception {
        mvc.perform(post("/api/v1/usuarios/" + principal.getId() + "/cerrar-sesiones").with(EL_PRINCIPAL))
                .andExpect(status().isNoContent());
    }

    @Test
    void otroAdminEditaConNormalidadAUnUsuarioComun() throws Exception {
        mvc.perform(put("/api/v1/usuarios/" + empleado.getId()).with(OTRO_ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(EDICION.formatted("it.emp.principal")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/usuarios/" + empleado.getId() + "/cerrar-sesiones").with(OTRO_ADMIN))
                .andExpect(status().isNoContent());
    }
}
