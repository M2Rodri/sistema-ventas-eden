package com.mitienda.ecommerce.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.nio.file.Path;

/**
 * Elige dónde se guardan los archivos subidos.
 *
 *   SUPABASE_URL y SUPABASE_SERVICE_KEY definidas -> Supabase Storage.
 *   Sin ellas en desarrollo                        -> disco local (uploads/).
 *   Sin ellas en producción                        -> el backend no arranca.
 *
 * Las dos variables se leen solo del entorno (o de backend/.env en desarrollo).
 * Ningún archivo del repositorio contiene sus valores.
 */
@Configuration
public class AlmacenConfig {

    private static final Logger log = LoggerFactory.getLogger(AlmacenConfig.class);

    @Bean
    public AlmacenArchivos almacenArchivos(@Value("${SUPABASE_URL:}") String urlDelProyecto,
                                           @Value("${SUPABASE_SERVICE_KEY:}") String claveDeServicio,
                                           Environment entorno) {
        boolean hayUrl = !urlDelProyecto.isBlank();
        boolean hayClave = !claveDeServicio.isBlank();

        if (hayUrl && hayClave) {
            log.info("Almacenamiento de archivos: Supabase Storage");
            return new AlmacenSupabase(urlDelProyecto, claveDeServicio);
        }

        if (entorno.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "Falta configurar el almacenamiento de archivos. En producción hay que definir las variables "
                            + "de entorno SUPABASE_URL y SUPABASE_SERVICE_KEY (en Render: Environment). "
                            + "Sin ellas las fotos se perderían al redesplegar.");
        }

        if (hayUrl != hayClave) {
            throw new IllegalStateException(
                    "Almacenamiento de archivos mal configurado: SUPABASE_URL y SUPABASE_SERVICE_KEY van juntas. "
                            + "Defina las dos o ninguna.");
        }

        log.warn("Almacenamiento de archivos: DISCO LOCAL (uploads/). Solo para desarrollo; "
                + "defina SUPABASE_URL y SUPABASE_SERVICE_KEY para usar Supabase Storage.");
        return new AlmacenLocal(Path.of(System.getProperty("user.dir"), "uploads"));
    }
}
