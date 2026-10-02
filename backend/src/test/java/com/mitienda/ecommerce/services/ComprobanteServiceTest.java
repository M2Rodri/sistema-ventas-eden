package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ComprobanteRequest;
import com.mitienda.ecommerce.dto.ComprobanteResponse;
import com.mitienda.ecommerce.models.Comprobante;
import com.mitienda.ecommerce.models.TipoComprobante;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.ComprobanteRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * El número del comprobante (REC-2025-00001) lleva el año de la FECHA DE LA
 * VENTA, no el año en que se genera ni uno fijo en el código.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComprobanteServiceTest {

    @Mock private ComprobanteRepository comprobanteRepository;
    @Mock private VentaRepository ventaRepository;

    private ComprobanteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ComprobanteService(comprobanteRepository, ventaRepository);
        when(comprobanteRepository.findByVentaId(anyLong())).thenReturn(Optional.empty());
        when(comprobanteRepository.saveAndFlush(any(Comprobante.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ComprobanteResponse crear(LocalDateTime fechaVenta, TipoComprobante tipo) {
        Venta venta = new Venta();
        venta.setId(9L);
        venta.setFechaVenta(fechaVenta);
        venta.setMontoTotal(new BigDecimal("135.00"));
        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));

        ComprobanteRequest pedido = new ComprobanteRequest();
        pedido.setIdVenta(9L);
        pedido.setTipoComprobante(tipo);
        return servicio.createComprobante(pedido);
    }

    @Test
    void elAnioSaleDeLaFechaDeLaVenta() {
        when(comprobanteRepository.findUltimoNumeroConPrefijo(anyString())).thenReturn(Optional.empty());

        assertEquals("REC-2025-00001", crear(LocalDateTime.of(2025, 12, 31, 23, 50), TipoComprobante.RECIBO).getNumeroComprobante());
        assertEquals("REC-2031-00001", crear(LocalDateTime.of(2031, 3, 5, 10, 0), TipoComprobante.RECIBO).getNumeroComprobante());
    }

    @Test
    void elCorrelativoSigueDentroDelMismoAnioYTipo() {
        when(comprobanteRepository.findUltimoNumeroConPrefijo(eq("REC-2026-"))).thenReturn(Optional.of("REC-2026-00007"));

        assertEquals("REC-2026-00008", crear(LocalDateTime.of(2026, 10, 1, 12, 0), TipoComprobante.RECIBO).getNumeroComprobante());
    }

    @Test
    void cadaTipoLlevaSuPrefijo() {
        when(comprobanteRepository.findUltimoNumeroConPrefijo(anyString())).thenReturn(Optional.empty());

        assertEquals("NV-2026-00001", crear(LocalDateTime.of(2026, 1, 2, 9, 0), TipoComprobante.NOTA_VENTA).getNumeroComprobante());
        assertEquals("COM-2026-00001", crear(LocalDateTime.of(2026, 1, 2, 9, 0), TipoComprobante.COMPROBANTE).getNumeroComprobante());
    }
}
