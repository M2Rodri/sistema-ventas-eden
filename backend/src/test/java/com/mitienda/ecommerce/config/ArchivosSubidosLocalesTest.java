package com.mitienda.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Con disco local, /uploads/** sigue abierta; un archivo inexistente responde 404. */
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
