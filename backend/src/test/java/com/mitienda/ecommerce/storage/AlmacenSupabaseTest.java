package com.mitienda.ecommerce.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba AlmacenSupabase contra un servidor falso que imita la API REST de
 * Supabase Storage: no hace falta conexión ni claves reales.
 */
class AlmacenSupabaseTest {

    private static final String CLAVE = "clave-de-prueba-123";

    private HttpServer servidor;
    private AlmacenSupabase almacen;
    private String urlBase;

    @TempDir
    Path carpetaLocal;

    private final List<String> peticiones = new ArrayList<>();
    private final List<String> cabeceras = new ArrayList<>();
    private int estadoARetornar = 200;
    private String cuerpoARetornar = "{}";
    private final AtomicInteger firmas = new AtomicInteger();

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            peticiones.add(intercambio.getRequestMethod() + " " + intercambio.getRequestURI().getRawPath());
            cabeceras.add(intercambio.getRequestHeaders().getFirst("Authorization") + "|"
                    + intercambio.getRequestHeaders().getFirst("apikey") + "|"
                    + intercambio.getRequestHeaders().getFirst("Content-Type"));
            if (intercambio.getRequestURI().getPath().contains("/object/sign/")) {
                firmas.incrementAndGet();
            }
            byte[] cuerpo = cuerpoARetornar.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estadoARetornar, cuerpo.length == 0 ? -1 : cuerpo.length);
            if (cuerpo.length > 0) {
                intercambio.getResponseBody().write(cuerpo);
            }
            intercambio.close();
        });
        servidor.start();
        urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
        almacen = new AlmacenSupabase(urlBase + "/", CLAVE, HttpClient.newHttpClient(), new AlmacenLocal(carpetaLocal));
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    @Test
    void subirALBucketPublicoDevuelveLaUrlPublicaYManda_laClaveSoloEnLasCabeceras() throws IOException {
        String referencia = almacen.guardar("productos", "abc.png", new byte[]{1, 2, 3}, "image/png");

        assertEquals(urlBase + "/storage/v1/object/public/productos/abc.png", referencia);
        assertEquals("POST /storage/v1/object/productos/abc.png", peticiones.get(0));
        assertEquals("Bearer " + CLAVE + "|" + CLAVE + "|image/png", cabeceras.get(0));
    }

    @Test
    void subirAlBucketPrivadoDevuelveSoloElNombreDelObjeto() throws IOException {
        String referencia = almacen.guardar("comprobantes", "pago.jpg", new byte[]{1}, "image/jpeg");

        assertEquals("pago.jpg", referencia);
        assertEquals("POST /storage/v1/object/comprobantes/pago.jpg", peticiones.get(0));
    }

    @Test
    void siSupabaseRechazaLaSubidaFallaSinMostrarLaClave() {
        estadoARetornar = 403;
        cuerpoARetornar = "{\"error\":\"Unauthorized\"}";

        IOException error = assertThrows(IOException.class,
                () -> almacen.guardar("productos", "x.png", new byte[]{1}, "image/png"));

        assertTrue(error.getMessage().contains("403"));
        assertFalse(error.getMessage().contains(CLAVE));
    }

    @Test
    void eliminarDesdeLaUrlPublicaBorraElObjetoDelBucket() throws IOException {
        almacen.eliminar("productos", urlBase + "/storage/v1/object/public/productos/abc.png");

        assertEquals("DELETE /storage/v1/object/productos/abc.png", peticiones.get(0));
    }

    @Test
    void eliminarUnArchivoQueYaNoEstaNoEsError() throws IOException {
        estadoARetornar = 404;
        almacen.eliminar("comprobantes", "ya-no-esta.png");
        assertEquals("DELETE /storage/v1/object/comprobantes/ya-no-esta.png", peticiones.get(0));
    }

    @Test
    void siSupabaseFallaAlEliminarSeAvisa() {
        estadoARetornar = 500;
        assertThrows(IOException.class, () -> almacen.eliminar("productos", "x.png"));
    }

    @Test
    void elBucketPrivadoEntregaUnaUrlFirmadaYLaReutiliza() {
        cuerpoARetornar = "{\"signedURL\":\"/object/sign/comprobantes/pago.jpg?token=TOKEN\"}";

        String primera = almacen.urlParaMostrar("comprobantes", "pago.jpg", Duration.ofHours(1));
        String segunda = almacen.urlParaMostrar("comprobantes", "pago.jpg", Duration.ofHours(1));

        assertEquals(urlBase + "/storage/v1/object/sign/comprobantes/pago.jpg?token=TOKEN", primera);
        assertEquals(primera, segunda);
        assertEquals(1, firmas.get(), "la segunda vez usa la URL ya firmada");
        assertEquals("POST /storage/v1/object/sign/comprobantes/pago.jpg", peticiones.get(0));
    }

    @Test
    void siNoSePuedeFirmarNoSeRompeNada() {
        estadoARetornar = 500;
        assertNull(almacen.urlParaMostrar("comprobantes", "pago.jpg", Duration.ofHours(1)));
    }

    @Test
    void elBucketPublicoNoNecesitaFirma() {
        String url = urlBase + "/storage/v1/object/public/productos/abc.png";
        assertEquals(url, almacen.urlParaMostrar("productos", url, Duration.ofHours(1)));
        assertTrue(peticiones.isEmpty());
    }

    @Test
    void losRegistrosViejosConservanSuRutaYSeBorranDelDiscoLocal() throws IOException {
        Path viejo = carpetaLocal.resolve("images").resolve("viejo.png");
        Files.createDirectories(viejo.getParent());
        Files.write(viejo, new byte[]{1});

        assertEquals("/uploads/images/viejo.png",
                almacen.urlParaMostrar("productos", "/uploads/images/viejo.png", Duration.ofHours(1)));

        almacen.eliminar("productos", "/uploads/images/viejo.png");

        assertFalse(Files.exists(viejo));
        assertTrue(peticiones.isEmpty(), "no se llama a Supabase por un archivo del disco");
    }
}
