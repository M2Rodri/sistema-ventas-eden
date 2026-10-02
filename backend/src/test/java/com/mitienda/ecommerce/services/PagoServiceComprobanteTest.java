package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.PeticionInvalidaException;
import com.mitienda.ecommerce.dto.PagoDTO;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.Pago;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.PagoRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class PagoServiceComprobanteTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};

    @Mock private PagoRepository pagoRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private AlmacenArchivos almacen;
    @Mock private UsuarioActualService usuarioActualService;

    private PagoService servicio;
    private Pago pago;

    @BeforeEach
    void preparar() {
        servicio = new PagoService(pagoRepository, ventaRepository, almacen, usuarioActualService);
        pago = new Pago();
        pago.setVenta(new Venta());
        pago.setMetodoPago(MetodoPago.QR);
        when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static MockMultipartFile foto() {
        return new MockMultipartFile("file", "comprobante.jpg", "image/jpeg", JPEG);
    }

    @Test
    void laFotoVaAlBucketPrivadoYSeGuardaSoloElNombreDelObjeto() throws IOException {
        when(almacen.guardar(eq("comprobantes"), anyString(), any(byte[].class), eq("image/jpeg")))
                .thenReturn("nuevo.jpg");

        PagoDTO resultado = servicio.adjuntarComprobante(3L, foto());

        assertEquals("nuevo.jpg", resultado.getUrlComprobante());
        assertFalse(resultado.isSinRespaldo());
    }

    @Test
    void alReemplazarSeEliminaLaFotoAnterior() throws IOException {
        pago.setUrlComprobante("anterior.jpg");
        when(almacen.guardar(anyString(), anyString(), any(byte[].class), anyString())).thenReturn("nuevo.jpg");

        servicio.adjuntarComprobante(3L, foto());

        verify(almacen).eliminar("comprobantes", "anterior.jpg");
    }

    @Test
    void siNoSePuedeBorrarLaAnteriorLaOperacionIgualFunciona() throws IOException {
        pago.setUrlComprobante("anterior.jpg");
        when(almacen.guardar(anyString(), anyString(), any(byte[].class), anyString())).thenReturn("nuevo.jpg");
        doThrow(new IOException("sin conexión")).when(almacen).eliminar(eq("comprobantes"), eq("anterior.jpg"));

        PagoDTO resultado = servicio.adjuntarComprobante(3L, foto());

        assertEquals("nuevo.jpg", resultado.getUrlComprobante());
    }

    @Test
    void unArchivoQueNoEsImagenNoLlegaAlAlmacenamiento() throws IOException {
        assertThrows(PeticionInvalidaException.class, () -> servicio.adjuntarComprobante(3L,
                new MockMultipartFile("file", "comprobante.jpg", "image/jpeg", "no soy imagen".getBytes())));

        verify(almacen, never()).guardar(anyString(), anyString(), any(byte[].class), anyString());
    }

    @Test
    void siLaBaseFallaSeLimpiaElArchivoSubido() throws IOException {
        when(almacen.guardar(anyString(), anyString(), any(byte[].class), anyString())).thenReturn("nuevo.jpg");
        when(pagoRepository.save(any(Pago.class))).thenThrow(new RuntimeException("base caída"));

        assertThrows(RuntimeException.class, () -> servicio.adjuntarComprobante(3L, foto()));

        verify(almacen).eliminar("comprobantes", "nuevo.jpg");
    }
}
