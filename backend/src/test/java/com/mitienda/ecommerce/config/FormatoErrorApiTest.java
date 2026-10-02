package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cada código HTTP de error sale con el formato único
 * { "error": { "codigo", "mensaje", "campos"? } }, con el código HTTP que corresponde.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FormatoErrorApiTest {

    private static final String BASE = "/api/v1/prueba-errores";
    private static final String ORIGEN = "http://localhost:3000";

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor EMPLEADO = user("empleado").roles("EMPLEADO");

    @Autowired
    private MockMvc mvc;

    private ResultActions error(ResultActions r, int estado, String codigo) throws Exception {
        return r.andExpect(status().is(estado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.codigo").value(codigo))
                .andExpect(jsonPath("$.error.mensaje").isNotEmpty());
    }

    // ---------- 400 ----------

    @Test
    void peticionInvalida_es400() throws Exception {
        error(mvc.perform(get(BASE + "/peticion").with(ADMIN)), 400, "ESTADO_INVALIDO")
                .andExpect(jsonPath("$.error.campos").doesNotExist());
    }

    @Test
    void validacionDeCampos_es400_conElDetalleDeCadaCampo() throws Exception {
        error(mvc.perform(post(BASE + "/validar").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"precio\":0}")), 400, "VALIDACION")
                .andExpect(jsonPath("$.error.campos.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.error.campos.precio").value("El precio debe ser mayor a 0"));
    }

    @Test
    void jsonMalFormado_es400() throws Exception {
        error(mvc.perform(post(BASE + "/validar").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                .content("{esto no es json")), 400, "PETICION_INVALIDA");
    }

    @Test
    void parametroDeTipoIncorrecto_es400() throws Exception {
        error(mvc.perform(get(BASE + "/numero/abc").with(ADMIN)), 400, "PETICION_INVALIDA");
    }

    // ---------- 401 y 403 ----------

    @Test
    void sinSesion_es401_conElFormatoUnico() throws Exception {
        error(mvc.perform(get(BASE + "/regla")), 401, "NO_AUTENTICADO");
    }

    @Test
    void rolSinPermisoPorRuta_es403_conElFormatoUnico() throws Exception {
        error(mvc.perform(get("/api/v1/reportes/financiero").with(EMPLEADO)), 403, "SIN_PERMISO");
    }

    @Test
    void rolSinPermisoPorAnotacion_es403_conElFormatoUnico() throws Exception {
        error(mvc.perform(get(BASE + "/solo-admin").with(EMPLEADO)), 403, "SIN_PERMISO");
        mvc.perform(get(BASE + "/solo-admin").with(ADMIN)).andExpect(status().isOk());
    }

    // ---------- 404 ----------

    @Test
    void recursoInexistente_es404_tambienEnPutPatchYDelete() throws Exception {
        for (HttpMethod metodo : new HttpMethod[]{HttpMethod.GET, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            error(mvc.perform(request(metodo, BASE + "/no-encontrado").with(ADMIN)), 404, "VENTA_NO_ENCONTRADA");
        }
    }

    @Test
    void rutaQueNoExiste_es404() throws Exception {
        error(mvc.perform(get("/api/v1/no-existe").with(ADMIN)), 404, "RUTA_NO_ENCONTRADA");
    }

    @Test
    void rutaVieja_es404_conElFormatoUnico() throws Exception {
        error(mvc.perform(get("/api/productos")), 404, "RUTA_NO_ENCONTRADA");
    }

    // ---------- 405 ----------

    @Test
    void metodoNoPermitido_es405_conElFormatoUnico() throws Exception {
        error(mvc.perform(request(HttpMethod.DELETE, BASE + "/regla").with(ADMIN)), 405, "METODO_NO_PERMITIDO");
    }

    // ---------- 409 y 422 ----------

    @Test
    void conflictoConElEstado_es409() throws Exception {
        error(mvc.perform(get(BASE + "/conflicto").with(ADMIN)), 409, "COMPRA_YA_CONFIRMADA");
    }

    @Test
    void reglaDeNegocio_es422() throws Exception {
        error(mvc.perform(get(BASE + "/regla").with(ADMIN)), 422, "STOCK_INSUFICIENTE")
                .andExpect(jsonPath("$.error.mensaje").value("Stock insuficiente para el producto 'Cama'"));
    }

    // ---------- 500 ----------

    @Test
    void errorInterno_es500_sinDetallesInternosNiTraza() throws Exception {
        error(mvc.perform(get(BASE + "/interno").with(ADMIN)), 500, "ERROR_INTERNO")
                .andExpect(content().string(not(containsString("tabla ventas"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("trace"))));
    }

    // ---------- CORS en las respuestas de error ----------

    @Test
    void lasRespuestasDeError_llevanLasCabecerasCors() throws Exception {
        String permitido = "Access-Control-Allow-Origin";
        // 404 de una ruta vieja: la responde un filtro anterior a Spring Security.
        mvc.perform(get("/api/productos").header("Origin", ORIGEN))
                .andExpect(status().isNotFound()).andExpect(header().string(permitido, ORIGEN));
        // 401 y 403 de la seguridad.
        mvc.perform(get(BASE + "/regla").header("Origin", ORIGEN))
                .andExpect(status().isUnauthorized()).andExpect(header().string(permitido, ORIGEN));
        mvc.perform(get("/api/v1/reportes/financiero").with(EMPLEADO).header("Origin", ORIGEN))
                .andExpect(status().isForbidden()).andExpect(header().string(permitido, ORIGEN));
        // 404, 422 y 500 de los controladores.
        for (String ruta : new String[]{"/no-encontrado", "/regla", "/interno"}) {
            mvc.perform(get(BASE + ruta).with(ADMIN).header("Origin", ORIGEN))
                    .andExpect(header().string(permitido, ORIGEN));
        }
    }

    @Test
    void laPreparacionCors_deUnaRutaVieja_noFalla() throws Exception {
        mvc.perform(options("/api/productos").header("Origin", ORIGEN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN));
    }
}
