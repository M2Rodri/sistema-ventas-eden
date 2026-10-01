package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.models.ImagenProducto;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.ImagenProductoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImagenProductoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Mock private ImagenProductoRepository imagenRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private AlmacenArchivos almacen;

    private ImagenProductoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ImagenProductoService(imagenRepository, productoRepository, almacen);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(new Producto()));
        when(imagenRepository.save(any(ImagenProducto.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void subirGuardaEnElBucketDeProductosYRegistraLaReferenciaDevuelta() throws IOException {
        when(almacen.guardar(eq("productos"), anyString(), any(byte[].class), eq("image/png")))
                .thenReturn("https://proyecto.supabase.co/storage/v1/object/public/productos/x.png");

        ImagenProducto guardada = servicio.saveImagenProducto(
                new MockMultipartFile("file", "foto.png", "image/png", PNG), 1L, true);

        assertEquals("https://proyecto.supabase.co/storage/v1/object/public/productos/x.png", guardada.getUrlImagen());
    }

    @Test
    void unArchivoQueNoEsImagenNoLlegaAlAlmacenamiento() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> servicio.saveImagenProducto(
                new MockMultipartFile("file", "foto.png", "image/png", "no soy imagen".getBytes()), 1L, false));

        verify(almacen, never()).guardar(anyString(), anyString(), any(byte[].class), anyString());
    }

    @Test
    void siLaBaseFallaSeLimpiaElArchivoSubido() throws IOException {
        when(almacen.guardar(anyString(), anyString(), any(byte[].class), anyString())).thenReturn("ref-1");
        when(imagenRepository.save(any(ImagenProducto.class))).thenThrow(new RuntimeException("base caída"));

        assertThrows(RuntimeException.class, () -> servicio.saveImagenProducto(
                new MockMultipartFile("file", "foto.png", "image/png", PNG), 1L, false));

        verify(almacen).eliminar("productos", "ref-1");
    }

    @Test
    void eliminarBorraElObjetoDelBucketYDespuesElRegistro() throws IOException {
        ImagenProducto imagen = new ImagenProducto();
        imagen.setUrlImagen("https://proyecto.supabase.co/storage/v1/object/public/productos/x.png");
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));

        servicio.deleteImagenProducto(5L);

        verify(almacen).eliminar("productos", imagen.getUrlImagen());
        verify(imagenRepository).deleteById(5L);
    }

    @Test
    void siNoSePuedeBorrarElObjetoElRegistroSeConservaParaReintentar() throws IOException {
        ImagenProducto imagen = new ImagenProducto();
        imagen.setUrlImagen("x.png");
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        doThrow(new IOException("sin conexión")).when(almacen).eliminar(anyString(), anyString());

        assertThrows(RuntimeException.class, () -> servicio.deleteImagenProducto(5L));

        verify(imagenRepository, never()).deleteById(5L);
    }
}
