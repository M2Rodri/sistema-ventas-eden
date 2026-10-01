package com.mitienda.ecommerce.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Guarda los archivos en la carpeta uploads/ del servidor.
 *
 * Sirve para desarrollo sin conexión a Supabase. En Render el disco se borra
 * al redesplegar o suspender, así que no se debe usar en producción: ahí
 * AlmacenConfig exige las variables de Supabase.
 *
 * También se usa para borrar y leer los archivos de registros viejos, cuya
 * referencia empieza con /uploads/.
 */
public class AlmacenLocal implements AlmacenArchivos {

    private final Path raiz;

    public AlmacenLocal(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    /** Carpetas de siempre, para que las rutas /uploads/... sigan igual. */
    private static String carpetaDe(String bucket) {
        return BUCKET_COMPROBANTES.equals(bucket) ? "comprobantes-pago" : "images";
    }

    @Override
    public String guardar(String bucket, String nombreObjeto, byte[] contenido, String tipoContenido) throws IOException {
        Path carpeta = raiz.resolve(carpetaDe(bucket)).normalize();
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(nombreObjeto).normalize();
        if (!destino.startsWith(carpeta)) {
            throw new IllegalArgumentException("Nombre de archivo no permitido: " + nombreObjeto);
        }
        Files.write(destino, contenido);
        return "/uploads/" + carpetaDe(bucket) + "/" + nombreObjeto;
    }

    @Override
    public void eliminar(String bucket, String referencia) throws IOException {
        Path ruta = resolver(referencia);
        if (ruta != null) {
            Files.deleteIfExists(ruta);
        }
    }

    @Override
    public byte[] leer(String bucket, String referencia) throws IOException {
        Path ruta = resolver(referencia);
        if (ruta == null || !Files.exists(ruta)) {
            throw new IOException("El archivo ya no existe: " + referencia);
        }
        return Files.readAllBytes(ruta);
    }

    @Override
    public String urlParaMostrar(String bucket, String referencia, Duration vigencia) {
        return referencia;
    }

    @Override
    public boolean esExterno() {
        return false;
    }

    /** Ruta en disco de una referencia "/uploads/...", o null si no es válida. */
    private Path resolver(String referencia) {
        if (referencia == null || !referencia.startsWith("/uploads/")) {
            return null;
        }
        Path ruta = raiz.resolve(referencia.substring("/uploads/".length())).normalize();
        return ruta.startsWith(raiz) ? ruta : null;
    }
}
