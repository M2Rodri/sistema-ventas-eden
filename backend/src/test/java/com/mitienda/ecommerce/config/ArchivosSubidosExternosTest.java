package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Con los archivos en Supabase Storage (producción), /uploads/** ya no se
 * sirve sin sesión: nada nuevo se pide por esa ruta y los comprobantes viejos
 * del disco no deben quedar al alcance de cualquiera.
 *
 * El servidor de Supabase es falso: nunca se le llama en esta prueba.
 */
@SpringBootTest(properties = {
        "SUPABASE_URL=http://127.0.0.1:9",
        "SUPABASE_SERVICE_KEY=clave-de-prueba"
})
@AutoConfigureMockMvc
class ArchivosSubidosExternosTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sinSesionNoSeSirveNadaDeUploads() throws Exception {
        for (String ruta : new String[]{"/uploads/comprobantes-pago/x.jpg", "/uploads/images/x.png"}) {
            int estado = mvc.perform(get(ruta)).andReturn().getResponse().getStatus();
            assertTrue(estado == 401 || estado == 403, ruta + " respondió " + estado + " en vez de exigir sesión");
        }
    }
}
