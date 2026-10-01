package com.mitienda.ecommerce.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda los archivos en Supabase Storage usando su API REST.
 *
 * La clave de servicio da acceso total al proyecto: solo la usa este backend y
 * se lee únicamente de la variable de entorno SUPABASE_SERVICE_KEY. Nunca se
 * manda al frontend ni a la app.
 *
 * Qué se guarda en la base de datos:
 *   bucket "productos" (público):    la URL pública del archivo.
 *   bucket "comprobantes" (privado): el nombre del objeto; la URL firmada se
 *                                    genera al responder, con vencimiento.
 *   registros viejos:                rutas "/uploads/..." del disco local.
 */
public class AlmacenSupabase implements AlmacenArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenSupabase.class);

    /** Una URL firmada se reutiliza hasta que le queden menos de estos minutos. */
    private static final Duration MARGEN_DE_REUSO = Duration.ofMinutes(10);

    private final String urlBase;
    private final String claveDeServicio;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final AlmacenLocal archivosViejos;

    private final Map<String, Firmada> firmadas = new ConcurrentHashMap<>();

    private record Firmada(String url, long venceEnMs) {
    }

    public AlmacenSupabase(String urlDelProyecto, String claveDeServicio) {
        this(urlDelProyecto, claveDeServicio,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                new AlmacenLocal(Path.of(System.getProperty("user.dir"), "uploads")));
    }

    AlmacenSupabase(String urlDelProyecto, String claveDeServicio, HttpClient http, AlmacenLocal archivosViejos) {
        this.urlBase = urlDelProyecto.trim().replaceAll("/+$", "");
        this.claveDeServicio = claveDeServicio.trim();
        this.http = http;
        this.archivosViejos = archivosViejos;
    }

    @Override
    public String guardar(String bucket, String nombreObjeto, byte[] contenido, String tipoContenido) throws IOException {
        HttpRequest peticion = base(rutaObjeto("object", bucket, nombreObjeto))
                .header("Content-Type", tipoContenido)
                .header("x-upsert", "false")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(contenido))
                .build();

        HttpResponse<byte[]> respuesta = enviar(peticion);
        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw falla("subir el archivo", respuesta);
        }
        return BUCKET_PRODUCTOS.equals(bucket) ? urlPublica(bucket, nombreObjeto) : nombreObjeto;
    }

    @Override
    public void eliminar(String bucket, String referencia) throws IOException {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        if (esViejo(referencia)) {
            archivosViejos.eliminar(bucket, referencia);
            return;
        }
        HttpRequest peticion = base(rutaObjeto("object", bucket, nombreObjeto(bucket, referencia)))
                .timeout(Duration.ofSeconds(30))
                .DELETE()
                .build();

        HttpResponse<byte[]> respuesta = enviar(peticion);
        // Si el archivo ya no estaba, el resultado buscado (que no esté) se cumple.
        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 404) {
            throw falla("eliminar el archivo", respuesta);
        }
    }

    @Override
    public byte[] leer(String bucket, String referencia) throws IOException {
        if (esViejo(referencia)) {
            return archivosViejos.leer(bucket, referencia);
        }
        HttpRequest peticion = base(rutaObjeto("object", bucket, nombreObjeto(bucket, referencia)))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> respuesta = enviar(peticion);
        if (respuesta.statusCode() != 200) {
            throw falla("leer el archivo", respuesta);
        }
        return respuesta.body();
    }

    @Override
    public String urlParaMostrar(String bucket, String referencia, Duration vigencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        if (esViejo(referencia)) {
            return referencia;
        }
        if (BUCKET_PRODUCTOS.equals(bucket)) {
            return referencia.startsWith("http") ? referencia : urlPublica(bucket, referencia);
        }
        return urlFirmada(bucket, nombreObjeto(bucket, referencia), vigencia);
    }

    @Override
    public boolean esExterno() {
        return true;
    }

    // ------------------------------------------------------------------

    private String urlFirmada(String bucket, String nombreObjeto, Duration vigencia) {
        String clave = bucket + "/" + nombreObjeto;
        long ahora = System.currentTimeMillis();

        Firmada guardada = firmadas.get(clave);
        if (guardada != null && guardada.venceEnMs() - ahora > MARGEN_DE_REUSO.toMillis()) {
            return guardada.url();
        }

        try {
            String cuerpo = json.writeValueAsString(Map.of("expiresIn", vigencia.toSeconds()));
            HttpRequest peticion = base(rutaObjeto("object/sign", bucket, nombreObjeto))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<byte[]> respuesta = enviar(peticion);
            if (respuesta.statusCode() != 200) {
                throw falla("firmar la URL", respuesta);
            }
            JsonNode firmada = json.readTree(respuesta.body()).path("signedURL");
            if (!firmada.isTextual()) {
                throw new IOException("Supabase Storage no devolvió la URL firmada");
            }
            String ruta = firmada.asText();
            String url = ruta.startsWith("http") ? ruta : urlBase + "/storage/v1" + ruta;
            firmadas.put(clave, new Firmada(url, ahora + vigencia.toMillis()));
            return url;
        } catch (IOException e) {
            // Sin URL el frontend simplemente no muestra el enlace; el resto del
            // sistema sigue funcionando.
            log.warn("No se pudo firmar la URL de {}: {}", clave, e.getMessage());
            return null;
        }
    }

    private HttpRequest.Builder base(String ruta) {
        return HttpRequest.newBuilder(URI.create(urlBase + "/storage/v1/" + ruta))
                .header("Authorization", "Bearer " + claveDeServicio)
                .header("apikey", claveDeServicio);
    }

    private HttpResponse<byte[]> enviar(HttpRequest peticion) throws IOException {
        try {
            return http.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Se interrumpió la comunicación con Supabase Storage", e);
        }
    }

    private IOException falla(String accion, HttpResponse<byte[]> respuesta) {
        String detalle = new String(respuesta.body(), StandardCharsets.UTF_8);
        if (detalle.length() > 200) {
            detalle = detalle.substring(0, 200);
        }
        // No se incluye la clave ni las cabeceras: solo el estado y el mensaje.
        return new IOException("No se pudo " + accion + " (Supabase Storage respondió "
                + respuesta.statusCode() + "): " + detalle);
    }

    private String urlPublica(String bucket, String nombreObjeto) {
        return urlBase + "/storage/v1/" + rutaObjeto("object/public", bucket, nombreObjeto);
    }

    private static String rutaObjeto(String prefijo, String bucket, String nombreObjeto) {
        StringBuilder ruta = new StringBuilder(prefijo).append('/').append(bucket);
        for (String parte : nombreObjeto.split("/")) {
            ruta.append('/').append(URLEncoder.encode(parte, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return ruta.toString();
    }

    /** Nombre del objeto dentro del bucket, a partir de lo guardado en la base. */
    private static String nombreObjeto(String bucket, String referencia) {
        String marca = "/object/public/" + bucket + "/";
        int posicion = referencia.indexOf(marca);
        if (referencia.startsWith("http") && posicion >= 0) {
            return referencia.substring(posicion + marca.length());
        }
        return referencia;
    }

    private static boolean esViejo(String referencia) {
        return referencia != null && referencia.startsWith("/uploads/");
    }
}
