package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un recurso que no existe responde 404 con el código propio de cada módulo, y también
 * cuando se intenta modificarlo (PUT, PATCH, DELETE). Antes varias de estas rutas
 * respondían 400 o 500.
 *
 * Usa las rutas, la validación y los servicios reales con un id que no existe, así que
 * no escribe nada en la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecursoInexistenteApiTest {

    private static final long INEXISTENTE = 999_999_999L;
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    private static final String CATEGORIA = "{\"nombre\":\"Camas\",\"tipoProducto\":\"CAMA\"}";
    private static final String CLIENTE = "{\"nombre\":\"Juan\",\"telefono\":\"70000000\"}";
    private static final String INVENTARIO = "{\"idProducto\":1,\"cantidadDisponible\":1}";
    private static final String PRODUCTO = "{\"sku\":\"X-1\",\"nombre\":\"Cama\",\"idCategoria\":1,\"precioVenta\":10,"
            + "\"tipoProducto\":\"CAMA\",\"activo\":true}";
    private static final String PROVEEDOR = "{\"nombreEmpresa\":\"Maderas\",\"nit\":\"1\",\"contacto\":\"Ana\",\"telefono\":\"7\"}";
    private static final String USUARIO = "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"usuario\":\"ana.perez\","
            + "\"role\":\"EMPLEADO\",\"activo\":true}";

    @Autowired
    private MockMvc mvc;

    private void noExiste(HttpMethod metodo, String ruta, String cuerpo, String codigo) throws Exception {
        var peticion = request(metodo, ruta.replace("{id}", String.valueOf(INEXISTENTE))).with(ADMIN);
        if (cuerpo != null) {
            peticion = peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        mvc.perform(peticion)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.codigo").value(codigo))
                .andExpect(jsonPath("$.error.mensaje").isNotEmpty());
    }

    @Test
    void ventas_pedirlasOModificarlas_inexistente_es404() throws Exception {
        noExiste(HttpMethod.GET, "/api/v1/ventas/{id}", null, "VENTA_NO_ENCONTRADA");
        noExiste(HttpMethod.PATCH, "/api/v1/ventas/{id}/cancelar", null, "VENTA_NO_ENCONTRADA");
        noExiste(HttpMethod.PATCH, "/api/v1/ventas/{id}/entregar", null, "VENTA_NO_ENCONTRADA");
        noExiste(HttpMethod.PATCH, "/api/v1/ventas/{id}/deshacer-entrega", null, "VENTA_NO_ENCONTRADA");
        noExiste(HttpMethod.PATCH, "/api/v1/ventas/{id}/datos-entrega", "{}", "VENTA_NO_ENCONTRADA");
    }

    @Test
    void productos_modificarlos_inexistente_es404() throws Exception {
        noExiste(HttpMethod.GET, "/api/v1/productos/{id}", null, "PRODUCTO_NO_ENCONTRADO");
        noExiste(HttpMethod.PUT, "/api/v1/productos/{id}", PRODUCTO, "PRODUCTO_NO_ENCONTRADO");
        noExiste(HttpMethod.DELETE, "/api/v1/productos/{id}", null, "PRODUCTO_NO_ENCONTRADO");
        noExiste(HttpMethod.PATCH, "/api/v1/productos/{id}/toggle-status", null, "PRODUCTO_NO_ENCONTRADO");
        noExiste(HttpMethod.PATCH, "/api/v1/productos/{id}/stock-minimo?stockMinimo=1", null, "PRODUCTO_NO_ENCONTRADO");
    }

    @Test
    void categorias_modificarlas_inexistente_es404() throws Exception {
        noExiste(HttpMethod.PUT, "/api/v1/categorias/{id}", CATEGORIA, "CATEGORIA_NO_ENCONTRADA");
        noExiste(HttpMethod.DELETE, "/api/v1/categorias/{id}", null, "CATEGORIA_NO_ENCONTRADA");
        noExiste(HttpMethod.PATCH, "/api/v1/categorias/{id}/toggle-status", null, "CATEGORIA_NO_ENCONTRADA");
    }

    @Test
    void clientesYProveedores_modificarlos_inexistente_es404() throws Exception {
        noExiste(HttpMethod.PUT, "/api/v1/clientes/{id}", CLIENTE, "CLIENTE_NO_ENCONTRADO");
        noExiste(HttpMethod.PUT, "/api/v1/proveedores/{id}", PROVEEDOR, "PROVEEDOR_NO_ENCONTRADO");
        noExiste(HttpMethod.PATCH, "/api/v1/proveedores/{id}/toggle-status", null, "PROVEEDOR_NO_ENCONTRADO");
    }

    @Test
    void compras_pedirlasOAnularlas_inexistente_es404() throws Exception {
        noExiste(HttpMethod.GET, "/api/v1/compras/{id}", null, "COMPRA_NO_ENCONTRADA");
        noExiste(HttpMethod.PATCH, "/api/v1/compras/{id}/cancelar", null, "COMPRA_NO_ENCONTRADA");
    }

    @Test
    void inventarioEImagenes_modificarlos_inexistente_es404() throws Exception {
        noExiste(HttpMethod.PUT, "/api/v1/inventario/{id}", INVENTARIO, "INVENTARIO_NO_ENCONTRADO");
        noExiste(HttpMethod.DELETE, "/api/v1/imagenes-producto/{id}", null, "IMAGEN_NO_ENCONTRADA");
        noExiste(HttpMethod.PUT, "/api/v1/imagenes-producto/{id}/principal/1", null, "IMAGEN_NO_ENCONTRADA");
    }

    @Test
    void usuarios_modificarlos_inexistente_es404() throws Exception {
        noExiste(HttpMethod.GET, "/api/v1/usuarios/{id}", null, "USUARIO_NO_ENCONTRADO");
        noExiste(HttpMethod.PUT, "/api/v1/usuarios/{id}", USUARIO, "USUARIO_NO_ENCONTRADO");
        noExiste(HttpMethod.DELETE, "/api/v1/usuarios/{id}", null, "USUARIO_NO_ENCONTRADO");
        noExiste(HttpMethod.PATCH, "/api/v1/usuarios/{id}/toggle-status", null, "USUARIO_NO_ENCONTRADO");
    }

    // ---------- 400 en las rutas reales ----------

    @Test
    void validacionDeCampos_enUnaRutaReal_es400_conElDetalleDeCadaCampo() throws Exception {
        mvc.perform(post("/api/v1/ventas").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.error.campos").isMap());
    }

    @Test
    void estadoDeVentaInvalido_es400() throws Exception {
        mvc.perform(get("/api/v1/ventas/estado/XYZ").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.codigo").value("ESTADO_INVALIDO"));
    }
}
