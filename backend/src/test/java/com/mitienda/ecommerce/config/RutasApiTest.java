package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Las rutas de la API van con /api/v1, las viejas ya no existen y el acceso por
 * rol se sigue aplicando a las rutas nuevas.
 *
 * Usa la aplicación completa (con la seguridad real) y simula el usuario con
 * su rol: no hace falta iniciar sesión.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RutasApiTest {

    @Autowired
    private MockMvc mvc;

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor EMPLEADO = user("empleado").roles("EMPLEADO");

    private int estado(HttpMethod metodo, String ruta, RequestPostProcessor usuario) throws Exception {
        var peticion = request(metodo, ruta).contentType("application/json").content("{}");
        if (usuario != null) {
            peticion = peticion.with(usuario);
        }
        return mvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    // ---------- Rutas viejas ----------

    @Test
    void unaRutaVieja_sinVersion_respondeNoEncontradoSinSesion() throws Exception {
        for (String ruta : new String[]{"/api/productos", "/api/auth/login", "/api/ventas/1", "/api/reportes/financiero",
                "/api/envios", "/api/auditorias", "/api/multimedia-productos/health", "/api/mensajes-contacto"}) {
            assertEquals(404, estado(HttpMethod.GET, ruta, null), ruta + " sin sesión");
        }
    }

    @Test
    void unaRutaVieja_sinVersion_respondeNoEncontradoTambienConSesionDeAdmin() throws Exception {
        for (String ruta : new String[]{"/api/productos", "/api/ventas", "/api/usuarios", "/api/promociones"}) {
            assertEquals(404, estado(HttpMethod.GET, ruta, ADMIN), ruta + " con ADMIN");
        }
    }

    // ---------- Rutas nuevas: sin sesión ----------

    @Test
    void lasRutasProtegidasNuevasExigenSesion() throws Exception {
        for (String ruta : new String[]{"/api/v1/productos", "/api/v1/categorias", "/api/v1/ventas", "/api/v1/pagos",
                "/api/v1/clientes", "/api/v1/compras", "/api/v1/proveedores", "/api/v1/usuarios",
                "/api/v1/dashboard/estadisticas", "/api/v1/dashboard/ventas-semanal", "/api/v1/reportes/financiero",
                "/api/v1/inventario", "/api/v1/inventario/catalogo", "/api/v1/comprobantes/venta/1",
                "/api/v1/imagenes-producto/producto/1"}) {
            assertEquals(401, estado(HttpMethod.GET, ruta, null), ruta + " sin sesión");
        }
    }

    @Test
    void lasEscriturasNuevasTambienExigenSesion() throws Exception {
        for (String ruta : new String[]{"/api/v1/productos", "/api/v1/ventas", "/api/v1/pagos", "/api/v1/clientes",
                "/api/v1/compras", "/api/v1/usuarios", "/api/v1/inventario/ajustar"}) {
            assertEquals(401, estado(HttpMethod.POST, ruta, null), ruta + " sin sesión");
        }
    }

    // ---------- Rutas nuevas: por rol ----------

    @Test
    void elEmpleadoNoPuedeEntrarARutasDeAdmin() throws Exception {
        for (String ruta : new String[]{"/api/v1/reportes/financiero", "/api/v1/reportes/ventas",
                "/api/v1/reportes/cuentas-por-cobrar", "/api/v1/usuarios", "/api/v1/compras", "/api/v1/proveedores"}) {
            assertEquals(403, estado(HttpMethod.GET, ruta, EMPLEADO), ruta + " con EMPLEADO");
        }
    }

    @Test
    void elEmpleadoNoPuedeEscribirProductosNiAjustarInventario() throws Exception {
        assertEquals(403, estado(HttpMethod.POST, "/api/v1/productos", EMPLEADO));
        assertEquals(403, estado(HttpMethod.DELETE, "/api/v1/productos/1", EMPLEADO));
        assertEquals(403, estado(HttpMethod.POST, "/api/v1/inventario/ajustar", EMPLEADO));
    }

    @Test
    void elEmpleadoSiPuedeLeerYVender() throws Exception {
        for (String ruta : new String[]{"/api/v1/productos", "/api/v1/ventas", "/api/v1/inventario",
                "/api/v1/clientes", "/api/v1/dashboard/estadisticas"}) {
            int estado = estado(HttpMethod.GET, ruta, EMPLEADO);
            assertNotEquals(401, estado, ruta);
            assertNotEquals(403, estado, ruta);
            assertNotEquals(404, estado, ruta);
        }
    }

    @Test
    void elAdminPuedeEntrarALosReportes() throws Exception {
        for (String ruta : new String[]{"/api/v1/reportes/financiero", "/api/v1/usuarios", "/api/v1/compras"}) {
            int estado = estado(HttpMethod.GET, ruta, ADMIN);
            assertNotEquals(401, estado, ruta);
            assertNotEquals(403, estado, ruta);
            assertNotEquals(404, estado, ruta);
        }
    }

    @Test
    void laRutaDePruebaPublicaDeAuthYaNoExiste() throws Exception {
        assertEquals(404, estado(HttpMethod.GET, "/api/v1/auth/test", null));
    }

    // ---------- Públicas ----------

    @Test
    void lasRutasPublicasSiguenAbiertas() throws Exception {
        assertEquals(200, estado(HttpMethod.GET, "/api/v1/salud", null));
        // El inicio de sesión llega al controlador (no 404, no 401 por falta de sesión).
        int login = estado(HttpMethod.POST, "/api/v1/auth/login", null);
        assertNotEquals(404, login);
        assertNotEquals(403, login);
    }
}
