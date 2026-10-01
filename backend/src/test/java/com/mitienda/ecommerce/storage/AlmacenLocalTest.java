package com.mitienda.ecommerce.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenLocalTest {

    @TempDir
    Path raiz;

    @Test
    void guardaEnLasCarpetasDeSiempreYDevuelveLaRutaRelativa() throws IOException {
        AlmacenLocal almacen = new AlmacenLocal(raiz);

        String foto = almacen.guardar("productos", "a.png", new byte[]{1, 2}, "image/png");
        String comprobante = almacen.guardar("comprobantes", "b.jpg", new byte[]{3}, "image/jpeg");

        assertEquals("/uploads/images/a.png", foto);
        assertEquals("/uploads/comprobantes-pago/b.jpg", comprobante);
        assertArrayEquals(new byte[]{1, 2}, almacen.leer("productos", foto));
        assertEquals(foto, almacen.urlParaMostrar("productos", foto, Duration.ofHours(1)));
    }

    @Test
    void eliminaElArchivoYNoFallaSiYaNoEstaba() throws IOException {
        AlmacenLocal almacen = new AlmacenLocal(raiz);
        String ref = almacen.guardar("productos", "a.png", new byte[]{1}, "image/png");

        almacen.eliminar("productos", ref);
        almacen.eliminar("productos", ref);

        assertFalse(Files.exists(raiz.resolve("images").resolve("a.png")));
    }

    @Test
    void noSaleDeLaCarpetaDeSubidas() throws IOException {
        AlmacenLocal almacen = new AlmacenLocal(raiz);
        Path ajeno = raiz.resolveSibling("ajeno-" + System.nanoTime() + ".txt");
        Files.write(ajeno, new byte[]{9});
        try {
            almacen.eliminar("productos", "/uploads/../" + ajeno.getFileName());
            assertTrue(Files.exists(ajeno), "no debe borrar fuera de uploads/");
            assertThrows(IllegalArgumentException.class,
                    () -> almacen.guardar("productos", "../escape.png", new byte[]{1}, "image/png"));
        } finally {
            Files.deleteIfExists(ajeno);
        }
    }
}
