package com.mitienda.ecommerce.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.RoleRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.services.RegistroAuditoria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** El costo y la ganancia de las ventas no llegan al Empleado en ninguna ruta de lectura. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentasCostosIntegracionTest {
    private static final String CLAVE = "clave-de-prueba-123";
    private static final List<String> CAMPOS_DE_COSTO = List.of("costoTotal", "ganancia", "costoUnitario");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder codificador;

    /** La auditoría se guarda en una transacción aparte que @Transactional no revierte (ver SesionesIntegracionTest). */
    @MockitoBean private RegistroAuditoria registroAuditoria;

    private String iniciarSesionComo(String rol) throws Exception {
        String nombre = "it.costo." + rol.toLowerCase().substring(0, 3) + "." + (System.nanoTime() % 1_000_000_000L);
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Prueba", "Costos", nombre,
                codificador.encode(CLAVE), roles.findByNombre(rol).orElseThrow()));
        String cuerpo = json.writeValueAsString(Map.of("usuario", usuario.getUsuario(), "password", CLAVE));
        var resultado = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode leer(String ruta, String token) throws Exception {
        var resultado = mvc.perform(get(ruta).header("Authorization", "Bearer " + token)).andReturn();
        assertEquals(200, resultado.getResponse().getStatus(), "GET " + ruta);
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    /** Todos los campos de costo con valor, en cualquier parte de la respuesta (incluidas las líneas). */
    private void buscarCostos(JsonNode nodo, String ruta, List<String> encontrados) {
        if (nodo.isObject()) {
            nodo.fields().forEachRemaining(campo -> {
                if (CAMPOS_DE_COSTO.contains(campo.getKey()) && !campo.getValue().isNull()) {
                    encontrados.add(ruta + " -> " + campo.getKey() + "=" + campo.getValue());
                }
                buscarCostos(campo.getValue(), ruta, encontrados);
            });
        } else if (nodo.isArray()) {
            nodo.forEach(hijo -> buscarCostos(hijo, ruta, encontrados));
        }
    }

    private List<String> costosEn(String ruta, String token) throws Exception {
        List<String> encontrados = new ArrayList<>();
        buscarCostos(leer(ruta, token), ruta, encontrados);
        return encontrados;
    }

    @Test
    void elEmpleadoNoRecibeCostosEnNingunaRutaDeLecturaDeVentas() throws Exception {
        String admin = iniciarSesionComo("ADMIN");
        String empleado = iniciarSesionComo("EMPLEADO");
        JsonNode ventas = leer("/api/v1/ventas", admin);
        assumeTrue(ventas.size() > 0, "no hay ventas en la base local: nada que comprobar");

        long id = ventas.get(0).get("id").asLong();
        long idCliente = ventas.get(0).get("idCliente").asLong();

        List<String> filtraciones = new ArrayList<>();
        for (String ruta : List.of(
                "/api/v1/ventas",
                "/api/v1/ventas/" + id,
                "/api/v1/ventas/cliente/" + idCliente,
                "/api/v1/ventas/estado/COMPLETADA",
                "/api/v1/ventas/estado/PENDIENTE_PAGO",
                "/api/v1/ventas/estado/CANCELADA",
                "/api/v1/ventas/del-dia",
                "/api/v1/ventas/ultimas")) {
            filtraciones.addAll(costosEn(ruta, empleado));
        }

        assertTrue(filtraciones.isEmpty(), "al Empleado le llegaron costos: " + filtraciones);
    }

    @Test
    void elAdminSiRecibeElCostoYLaGananciaDeLasVentas() throws Exception {
        String admin = iniciarSesionComo("ADMIN");
        JsonNode ventas = leer("/api/v1/ventas", admin);
        assumeTrue(ventas.size() > 0, "no hay ventas en la base local: nada que comprobar");

        long conCosto = 0;
        for (JsonNode venta : ventas) {
            if (venta.hasNonNull("costoTotal") && venta.hasNonNull("ganancia")) {
                conCosto++;
            }
        }
        assertTrue(conCosto > 0, "el Admin debería recibir costoTotal y ganancia en las ventas no canceladas");
    }
}
