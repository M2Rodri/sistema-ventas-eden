package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * En desarrollo con disco local, /uploads/** sigue abierta: las pantallas
 * muestran las fotos con <img>/<a href>, que no pueden mandar el token. Un
 * archivo que no existe responde 404, no 401 ni 403.
 */
@SpringBootTest(properties = {
        "SUPABASE_URL=",
        "SUPABASE_SERVICE_KEY="
})
@AutoConfigureMockMvc
class ArchivosSubidosLocalesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void conDiscoLocalLaRutaSigueAbierta() throws Exception {
        int estado = mvc.perform(get("/uploads/images/no-existe-" + System.nanoTime() + ".png"))
                .andReturn().getResponse().getStatus();
        assertEquals(404, estado);
    }
}
