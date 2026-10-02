package com.mitienda.ecommerce.storage;

import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Revisa en el servidor que un archivo subido sea de verdad una imagen
 * permitida y que no pase del tamaño máximo, antes de mandarlo al
 * almacenamiento.
 *
 * No se confía en lo que dice el navegador: además de la extensión y el tipo
 * declarado, se miran los primeros bytes del archivo (JPEG, PNG o WebP). El
 * nombre con el que se guarda lo elige el servidor (un UUID), así que el nombre
 * original nunca llega a la ruta.
 */
public final class ValidadorImagen {

    public static final long MAXIMO_FOTO_PRODUCTO = 5L * 1024 * 1024;
    public static final long MAXIMO_COMPROBANTE = 10L * 1024 * 1024;

    private static final Set<String> EXTENSIONES = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> TIPOS_DECLARADOS = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    /** Imagen ya revisada: su contenido, el tipo real y el nombre con el que se guarda. */
    public record ImagenValida(byte[] contenido, String tipoContenido, String nombreObjeto) {
    }

    private ValidadorImagen() {
    }

    public static ImagenValida validar(MultipartFile archivo, long maximoBytes) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new PeticionInvalidaException("ARCHIVO_VACIO", "El archivo no puede estar vacío");
        }
        if (archivo.getSize() > maximoBytes) {
            throw new PeticionInvalidaException("ARCHIVO_MUY_GRANDE",
                    "El archivo excede el tamaño máximo permitido de " + (maximoBytes / (1024 * 1024)) + " MB");
        }

        String extension = extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES.contains(extension)) {
            throw new PeticionInvalidaException("IMAGEN_INVALIDA",
                    "Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG o WebP");
        }

        String declarado = archivo.getContentType();
        if (declarado != null && !declarado.isBlank()
                && !TIPOS_DECLARADOS.contains(declarado.toLowerCase(Locale.ROOT))) {
            throw new PeticionInvalidaException("IMAGEN_INVALIDA",
                    "Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG o WebP");
        }

        byte[] contenido = archivo.getBytes();
        String tipoReal = tipoSegunContenido(contenido);
        if (tipoReal == null) {
            throw new PeticionInvalidaException("IMAGEN_INVALIDA", "El contenido del archivo no es una imagen JPG, PNG o WebP válida");
        }
        if (!extensionCoincide(extension, tipoReal)) {
            throw new PeticionInvalidaException("IMAGEN_INVALIDA", "La extensión del archivo no corresponde a su contenido");
        }

        return new ImagenValida(contenido, tipoReal, UUID.randomUUID() + extensionParaGuardar(tipoReal));
    }

    private static String extensionDe(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto).toLowerCase(Locale.ROOT);
    }

    /** Tipo real según los primeros bytes, o null si no es JPEG, PNG ni WebP. */
    static String tipoSegunContenido(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean extensionCoincide(String extension, String tipo) {
        return switch (tipo) {
            case "image/jpeg" -> extension.equals(".jpg") || extension.equals(".jpeg");
            case "image/png" -> extension.equals(".png");
            case "image/webp" -> extension.equals(".webp");
            default -> false;
        };
    }

    private static String extensionParaGuardar(String tipo) {
        return switch (tipo) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
