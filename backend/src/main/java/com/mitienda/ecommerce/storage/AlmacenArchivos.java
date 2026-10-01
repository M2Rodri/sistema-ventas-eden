package com.mitienda.ecommerce.storage;

import java.io.IOException;
import java.time.Duration;

/**
 * Dónde se guardan los archivos que suben los usuarios (fotos de productos y
 * fotos de comprobantes de pago).
 *
 * En producción es Supabase Storage (AlmacenSupabase). Sin las variables de
 * entorno, en desarrollo, se usa el disco local (AlmacenLocal).
 *
 * Lo que devuelve guardar() es lo que se escribe en la base de datos, y es lo
 * que hay que devolver después a eliminar(), leer() y urlParaMostrar().
 */
public interface AlmacenArchivos {

    /** Fotos del catálogo: bucket público. */
    String BUCKET_PRODUCTOS = "productos";

    /** Fotos de comprobantes de pago: bucket privado. */
    String BUCKET_COMPROBANTES = "comprobantes";

    /**
     * Sube un archivo y devuelve la referencia que se guarda en la base de
     * datos: la URL pública en el bucket público, o el nombre del objeto en el
     * bucket privado.
     */
    String guardar(String bucket, String nombreObjeto, byte[] contenido, String tipoContenido) throws IOException;

    /** Elimina el archivo. Si ya no existe, no es un error. */
    void eliminar(String bucket, String referencia) throws IOException;

    /** Lee el contenido del archivo. */
    byte[] leer(String bucket, String referencia) throws IOException;

    /**
     * URL con la que el navegador puede abrir el archivo. En el bucket
     * privado es una URL firmada que vence a los {@code vigencia}.
     */
    String urlParaMostrar(String bucket, String referencia, Duration vigencia);

    /** true si los archivos viven fuera de este servidor (Supabase). */
    boolean esExterno();
}
