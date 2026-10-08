package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ReporteFinancieroResponse;
import com.mitienda.ecommerce.dto.ReporteVentasResponse;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.CompraRepository;
import com.mitienda.ecommerce.repositories.DetalleVentaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** gananciaVentas del reporte de ventas: mismo cálculo que el financiero (solo COMPLETADA, con el costo guardado). */
@ExtendWith(MockitoExtension.class)
class ReporteVentasGananciaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 9, 30, 23, 59);

    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private CompraRepository compraRepository;

    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(ventaRepository, detalleVentaRepository, inventarioRepository, compraRepository);
    }

    private Venta venta(long id, EstadoVenta estado, String monto, String precio, String costo, int cantidad) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setEstado(estado);
        venta.setMontoTotal(new BigDecimal(monto));
        venta.setFechaVenta(LocalDateTime.of(2026, 9, 20, 10, 0));
        DetalleVenta detalle = new DetalleVenta();
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal(precio));
        detalle.setCostoUnitario(new BigDecimal(costo));
        venta.setDetalles(new ArrayList<>(List.of(detalle)));
        return venta;
    }

    @Test
    void sumaPrecioMenosCostoPorCantidad_soloDeVentasCompletadas() {
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(List.of(
                venta(1, EstadoVenta.COMPLETADA, "1000.00", "500.00", "300.00", 2),   // 400
                venta(2, EstadoVenta.COMPLETADA, "250.00", "250.00", "100.00", 1),    // 150
                venta(3, EstadoVenta.PENDIENTE_PAGO, "800.00", "800.00", "200.00", 1), // no cuenta
                venta(4, EstadoVenta.CANCELADA, "900.00", "900.00", "100.00", 1)));    // no cuenta

        ReporteVentasResponse reporte = servicio.getReporteVentas(INICIO, FIN);

        assertEquals(0, new BigDecimal("550.00").compareTo(reporte.getGananciaVentas()));
        assertEquals(3L, reporte.getTotalVentas());
    }

    @Test
    void sinVentas_laGananciaEsCero() {
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(List.of());

        ReporteVentasResponse reporte = servicio.getReporteVentas(INICIO, FIN);

        assertEquals(0, BigDecimal.ZERO.compareTo(reporte.getGananciaVentas()));
    }

    @Test
    void coincideConLaGananciaDelReporteFinanciero() {
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(List.of(
                venta(1, EstadoVenta.COMPLETADA, "1000.00", "500.00", "300.00", 2),
                venta(2, EstadoVenta.PENDIENTE_PAGO, "800.00", "800.00", "200.00", 1),
                venta(3, EstadoVenta.CANCELADA, "900.00", "900.00", "100.00", 1)));
        when(compraRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(), any())).thenReturn(List.of());

        ReporteVentasResponse ventas = servicio.getReporteVentas(INICIO, FIN);
        ReporteFinancieroResponse financiero = servicio.getReporteFinanciero(INICIO, FIN);

        assertEquals(0, financiero.getGananciaVentas().compareTo(ventas.getGananciaVentas()));
    }
}
