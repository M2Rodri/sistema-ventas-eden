package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Con Supabase Storage, /uploads/** ya no se sirve sin sesión. */
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
