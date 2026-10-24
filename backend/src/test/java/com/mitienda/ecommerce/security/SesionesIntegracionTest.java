package com.mitienda.ecommerce.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La sesión de punta a punta con la seguridad real: login, renovar, cortar, dar de baja y topes por origen. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SesionesIntegracionTest {
    private static final String CLAVE = "clave-de-prueba-123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder codificador;
    @Autowired private JwtUtil jwtUtil;

    /** La auditoría usa su propia transacción y no se revierte: se reemplaza para no dejar filas en la base local. */
    @MockitoBean private RegistroAuditoria registroAuditoria;

    private Usuario admin;
    private Usuario empleado;

    @BeforeEach
    void crearUsuarios() {
        admin = crear("it.admin", "ADMIN");
        empleado = crear("it.emp", "EMPLEADO");
    }

    private Usuario crear(String prefijo, String rol) {
        Role role = roles.findByNombre(rol).orElseThrow();
        String nombre = prefijo + "." + (System.nanoTime() % 1_000_000_000L);
        return usuarios.saveAndFlush(new Usuario("Prueba", "Integracion", nombre, codificador.encode(CLAVE), role));
    }

    // ---------- ayudas ----------

    private String iniciarSesion(Usuario usuario, String perfil) throws Exception {
        String cuerpo = json.writeValueAsString(perfil == null
                ? java.util.Map.of("usuario", usuario.getUsuario(), "password", CLAVE)
                : java.util.Map.of("usuario", usuario.getUsuario(), "password", CLAVE, "perfilSesion", perfil));
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private String iniciarSesion(Usuario usuario) throws Exception {
        return iniciarSesion(usuario, null);
    }

    private int estadoDeGet(String ruta, String token) throws Exception {
        var peticion = get(ruta);
        if (token != null) {
            peticion = peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    private int estadoDePost(String ruta, String token) throws Exception {
        return mvc.perform(post(ruta).header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus();
    }

    /** Una ruta que solo el ADMIN puede ver: sirve para comprobar quién es el que entra. */
    private static final String SOLO_ADMIN = "/api/v1/usuarios";

    private JsonNode datosDelToken(String token) throws Exception {
        String carga = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        return json.readTree(carga);
    }

    /** Las operaciones se hacen en el mismo milisegundo con facilidad: se espera un instante para ordenarlas. */
    private void esperarUnInstante() throws InterruptedException {
        Thread.sleep(10);
    }

    // ---------- iniciar sesión y roles ----------

    @Test
    void sinTokenOConUnTokenInvalidoResponde401() throws Exception {
        assertEquals(401, estadoDeGet(SOLO_ADMIN, null));
        assertEquals(401, estadoDeGet(SOLO_ADMIN, "esto.no.esUnToken"));
    }

    @Test
    void elAdminEntraYElEmpleadoRecibe403EnUnaRutaDeAdmin() throws Exception {
        assertEquals(200, estadoDeGet(SOLO_ADMIN, iniciarSesion(admin)));
        assertEquals(403, estadoDeGet(SOLO_ADMIN, iniciarSesion(empleado)));
    }

    @Test
    void elTokenDelLoginDuraLoQueDiceLaConfiguracion() throws Exception {
        JsonNode datos = datosDelToken(iniciarSesion(admin));

        assertEquals(4 * 60 * 60, datos.get("exp").asLong() - datos.get("iat").asLong(), "el token debe durar 4 horas");
    }

    // ---------- topes según desde dónde se entra ----------

    @Test
    void cadaTipoDeClienteRecibeSuTopeDeSesion() throws Exception {
        long hora = 60L * 60 * 1000;

        assertEquals(8 * hora, datosDelToken(iniciarSesion(admin)).get("tope").asLong());
        assertEquals(12 * hora, datosDelToken(iniciarSesion(admin, JwtUtil.PERFIL_MOVIL_CON_CANDADO)).get("tope").asLong());
        assertEquals(4 * hora, datosDelToken(iniciarSesion(admin, JwtUtil.PERFIL_MOVIL_SIN_CANDADO)).get("tope").asLong());
    }

    @Test
    void laRenovacionConservaElTopeDelPerfil() throws Exception {
        String token = iniciarSesion(admin, JwtUtil.PERFIL_MOVIL_CON_CANDADO);

        MvcResult renovado = mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String nuevo = json.readTree(renovado.getResponse().getContentAsString()).get("token").asText();

        assertEquals(12L * 60 * 60 * 1000, datosDelToken(nuevo).get("tope").asLong());
        assertEquals(200, estadoDeGet(SOLO_ADMIN, nuevo));
    }

    // ---------- renovar ----------

    @Test
    void renovarDevuelveUnTokenQueSirve() throws Exception {
        String token = iniciarSesion(admin);

        MvcResult renovado = mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode cuerpo = json.readTree(renovado.getResponse().getContentAsString());

        assertEquals(admin.getUsuario(), cuerpo.get("usuario").asText());
        assertEquals("ADMIN", cuerpo.get("role").asText());
        assertEquals(200, estadoDeGet(SOLO_ADMIN, cuerpo.get("token").asText()));
    }

    @Test
    void renovarSinTokenOConTokenInvalidoResponde401() throws Exception {
        mvc.perform(post("/api/v1/auth/renovar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer basura.basura.basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeAntesDelCambioSinInicioNiTopeSigueFuncionandoYSeRenueva() throws Exception {
        // Como los tokens de 12 horas que ya están en uso cuando se publique esta versión.
        String antiguo = jwtUtil.generateToken(admin.getUsuario());

        assertEquals(200, estadoDeGet(SOLO_ADMIN, antiguo));
        assertEquals(200, mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + antiguo))
                .andReturn().getResponse().getStatus());
    }

    // ---------- el rol sale de la base ----------

    @Test
    void siLeBajanElRolAlTokenViejoLeRigeElNuevoYRenovarLoReflejaEnElTokenNuevo() throws Exception {
        String tokenComoAdmin = iniciarSesion(admin);
        assertEquals(200, estadoDeGet(SOLO_ADMIN, tokenComoAdmin));

        admin.setRol(roles.findByNombre("EMPLEADO").orElseThrow());
        usuarios.saveAndFlush(admin);

        // El token sigue diciendo ADMIN, pero el rol que vale es el de la base.
        assertEquals(403, estadoDeGet(SOLO_ADMIN, tokenComoAdmin));

        MvcResult renovado = mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + tokenComoAdmin))
                .andExpect(status().isOk()).andReturn();
        assertEquals("EMPLEADO", json.readTree(renovado.getResponse().getContentAsString()).get("role").asText());
    }

    // ---------- la app móvil es solo para administradores ----------

    @Test
    void unEmpleadoNoPuedeIniciarSesionEnLaApp() throws Exception {
        for (String perfil : new String[] {JwtUtil.PERFIL_MOVIL_CON_CANDADO, JwtUtil.PERFIL_MOVIL_SIN_CANDADO}) {
            String cuerpo = json.writeValueAsString(java.util.Map.of(
                    "usuario", empleado.getUsuario(), "password", CLAVE, "perfilSesion", perfil));
            MvcResult resultado = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden())
                    .andReturn();
            assertEquals("APP_SOLO_ADMIN",
                    json.readTree(resultado.getResponse().getContentAsString()).get("error").get("codigo").asText());
        }
    }

    @Test
    void elEmpleadoSiEntraALaWebYElAdminALaApp() throws Exception {
        assertEquals(403, estadoDeGet(SOLO_ADMIN, iniciarSesion(empleado)));
        assertEquals(200, estadoDeGet(SOLO_ADMIN, iniciarSesion(admin, JwtUtil.PERFIL_MOVIL_CON_CANDADO)));
    }

    @Test
    void siLeQuitanElRolAUnAdminSuSesionDeLaAppDejaDeServir() throws Exception {
        String tokenApp = iniciarSesion(admin, JwtUtil.PERFIL_MOVIL_CON_CANDADO);
        assertEquals(200, estadoDeGet(SOLO_ADMIN, tokenApp));

        admin.setRol(roles.findByNombre("EMPLEADO").orElseThrow());
        usuarios.saveAndFlush(admin);

        assertEquals(401, estadoDeGet("/api/v1/productos", tokenApp));
        mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + tokenApp))
                .andExpect(status().isUnauthorized());
    }

    // ---------- dar de baja ----------

    @Test
    void unUsuarioDadoDeBajaPierdeElAccesoDeInmediatoAunqueSuTokenSigaVigente() throws Exception {
        String token = iniciarSesion(admin);
        assertEquals(200, estadoDeGet(SOLO_ADMIN, token));

        admin.setActivo(false);
        usuarios.saveAndFlush(admin);

        assertEquals(401, estadoDeGet(SOLO_ADMIN, token));
        mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- cortar sesiones ----------

    @Test
    void cerrarMisSesionesInvalidaElTokenViejoPeroNoUnInicioDeSesionNuevo() throws Exception {
        String viejo = iniciarSesion(empleado);
        assertEquals(200, estadoDeGet("/api/v1/categorias", viejo));
        esperarUnInstante();

        assertEquals(204, estadoDePost("/api/v1/auth/cerrar-sesiones", viejo));

        assertEquals(401, estadoDeGet("/api/v1/categorias", viejo));
        mvc.perform(post("/api/v1/auth/renovar").header("Authorization", "Bearer " + viejo))
                .andExpect(status().isUnauthorized());

        // Iniciar sesión de nuevo sí funciona.
        String nuevo = iniciarSesion(empleado);
        assertEquals(200, estadoDeGet("/api/v1/categorias", nuevo));
    }

    @Test
    void elAdminCierraLasSesionesDeOtroUsuarioSinCerrarLasSuyas() throws Exception {
        String tokenAdmin = iniciarSesion(admin);
        String tokenEmpleado = iniciarSesion(empleado);
        esperarUnInstante();

        assertEquals(204, estadoDePost("/api/v1/usuarios/" + empleado.getId() + "/cerrar-sesiones", tokenAdmin));

        assertEquals(401, estadoDeGet("/api/v1/categorias", tokenEmpleado));
        assertEquals(200, estadoDeGet(SOLO_ADMIN, tokenAdmin));
    }

    @Test
    void unEmpleadoNoPuedeCerrarLasSesionesDeOtro() throws Exception {
        String tokenEmpleado = iniciarSesion(empleado);
        String tokenAdmin = iniciarSesion(admin);
        esperarUnInstante();

        assertEquals(403, estadoDePost("/api/v1/usuarios/" + admin.getId() + "/cerrar-sesiones", tokenEmpleado));

        assertEquals(200, estadoDeGet(SOLO_ADMIN, tokenAdmin));
    }

    @Test
    void cambiarLaContrasenaDeUnUsuarioCortaSusSesionesAbiertas() throws Exception {
        String tokenAdmin = iniciarSesion(admin);
        String tokenEmpleado = iniciarSesion(empleado);
        assertEquals(200, estadoDeGet("/api/v1/categorias", tokenEmpleado));
        esperarUnInstante();

        String cuerpo = json.writeValueAsString(java.util.Map.of(
                "nombre", "Prueba", "apellido", "Integracion", "usuario", empleado.getUsuario(),
                "role", "EMPLEADO", "activo", true, "password", "otra-clave-456"));
        mvc.perform(put("/api/v1/usuarios/" + empleado.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());

        assertEquals(401, estadoDeGet("/api/v1/categorias", tokenEmpleado));
        assertTrue(estadoDeGet(SOLO_ADMIN, tokenAdmin) == 200, "las sesiones del admin no se tocan");
    }

    @Test
    void editarUnUsuarioSinCambiarLaContrasenaNoCortaSusSesiones() throws Exception {
        String tokenAdmin = iniciarSesion(admin);
        String tokenEmpleado = iniciarSesion(empleado);
        esperarUnInstante();

        String cuerpo = json.writeValueAsString(java.util.Map.of(
                "nombre", "Prueba", "apellido", "Cambiado", "usuario", empleado.getUsuario(),
                "role", "EMPLEADO", "activo", true));
        mvc.perform(put("/api/v1/usuarios/" + empleado.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());

        assertEquals(200, estadoDeGet("/api/v1/categorias", tokenEmpleado));
    }
}
