package com.mitienda.ecommerce.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorImagenTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};

    private static MockMultipartFile archivo(String nombre, String tipo, byte[] contenido) {
        return new MockMultipartFile("file", nombre, tipo, contenido);
    }

    @Test
    void aceptaJpegPngYWebpYGuardaConNombreDelServidor() throws IOException {
        var jpeg = ValidadorImagen.validar(archivo("foto.JPG", "image/jpeg", JPEG), 1024);
        var png = ValidadorImagen.validar(archivo("foto.png", "image/png", PNG), 1024);
        var webp = ValidadorImagen.validar(archivo("foto.webp", "image/webp", WEBP), 1024);

        assertEquals("image/jpeg", jpeg.tipoContenido());
        assertTrue(jpeg.nombreObjeto().endsWith(".jpg"));
        assertTrue(png.nombreObjeto().endsWith(".png"));
        assertTrue(webp.nombreObjeto().endsWith(".webp"));
        assertFalse(jpeg.nombreObjeto().contains("foto"), "el nombre original no llega a la ruta");
    }

    @Test
    void rechazaUnArchivoVacioONulo() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("a.png", "image/png", new byte[0]), 1024));
        assertThrows(IllegalArgumentException.class, () -> ValidadorImagen.validar(null, 1024));
    }

    @Test
    void rechazaLoQueExcedeElTamanoMaximo() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("a.png", "image/png", PNG), 4));
        assertTrue(error.getMessage().contains("tamaño máximo"));
    }

    @Test
    void rechazaUnaExtensionNoPermitida() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("virus.exe", "image/png", PNG), 1024));
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("sin_extension", "image/png", PNG), 1024));
    }

    @Test
    void rechazaUnTipoDeclaradoNoPermitido() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("a.png", "application/pdf", PNG), 1024));
    }

    @Test
    void rechazaUnArchivoCuyoContenidoNoEsUnaImagenAunqueSeLlamePng() {
        byte[] texto = "<?php echo 'hola'; ?>".getBytes();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("a.png", "image/png", texto), 1024));
        assertTrue(error.getMessage().contains("contenido"));
    }

    @Test
    void rechazaUnaExtensionQueNoCorrespondeAlContenido() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidadorImagen.validar(archivo("a.png", "image/png", JPEG), 1024));
    }
}
