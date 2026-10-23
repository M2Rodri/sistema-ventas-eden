package com.mitienda.ecommerce.storage;

import java.io.IOException;
import java.time.Duration;

/** Dónde se guardan los archivos subidos: Supabase Storage en producción, disco local en desarrollo. */
public interface AlmacenArchivos {
    /** Fotos del catálogo: bucket público. */
    String BUCKET_PRODUCTOS = "productos";

    /** Fotos de comprobantes de pago: bucket privado. */
    String BUCKET_COMPROBANTES = "comprobantes";

    /** Sube un archivo y devuelve lo que se guarda en la base (URL pública o nombre del objeto privado). */
    String guardar(String bucket, String nombreObjeto, byte[] contenido, String tipoContenido) throws IOException;

    /** Elimina el archivo. Si ya no existe, no es un error. */
    void eliminar(String bucket, String referencia) throws IOException;

    /** Lee el contenido del archivo. */
    byte[] leer(String bucket, String referencia) throws IOException;

    /** URL para abrir el archivo; en el bucket privado es firmada y vence. */
    String urlParaMostrar(String bucket, String referencia, Duration vigencia);

    /** true si los archivos viven fuera de este servidor (Supabase). */
    boolean esExterno();
}
