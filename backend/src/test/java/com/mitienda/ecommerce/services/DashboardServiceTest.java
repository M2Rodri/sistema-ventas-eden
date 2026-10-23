package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.dto.VentasSemanalResponse;
import com.mitienda.ecommerce.models.EstadoAlerta;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.AlertaInventarioRepository;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.DetalleVentaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Las cifras del panel de inicio: ventas COMPLETADA, valor del inventario solo para ADMIN y la semana de lunes a domingo. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private AlertaInventarioRepository alertaInventarioRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private UsuarioActualService usuarioActualService;

    private DashboardService servicio;

    @BeforeEach
    void preparar() {
        servicio = new DashboardService(ventaRepository, productoRepository, inventarioRepository,
                clienteRepository, alertaInventarioRepository, detalleVentaRepository, usuarioActualService);

        when(inventarioRepository.resumenInventario()).thenReturn(Collections.singletonList(new Object[]{2L, 3L, new BigDecimal("1500.50")}));
        when(productoRepository.resumenConteo()).thenReturn(Collections.singletonList(new Object[]{12L, 10L}));
        when(alertaInventarioRepository.countByEstado(EstadoAlerta.PENDIENTE)).thenReturn(4L);
        when(clienteRepository.resumenClientes(any(), any(), any())).thenReturn(Collections.singletonList(new Object[]{20L, 18L, 1L, 5L}));
        when(ventaRepository.resumenPorCobrar()).thenReturn(Collections.singletonList(new Object[]{3L, new BigDecimal("250.00")}));
        when(ventaRepository.countVentasPorEntregar()).thenReturn(6L);
        when(ventaRepository.findResumenVentasEntre(any(), any())).thenReturn(Collections.emptyList());
        when(detalleVentaRepository.findMasVendidosEntre(any(), any(), any())).thenReturn(Collections.emptyList());
        ventasDelMes(4L, "400.00");
    }

    private void ventasDelMes(long cantidad, String monto) {
        when(ventaRepository.resumenVentasCompletadas(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(
                        new Object[]{1L, new BigDecimal("100.00"), cantidad, new BigDecimal(monto), 10L, new BigDecimal("1000.00")}));
    }

    private Venta venta(String fecha, EstadoVenta estado, String monto) {
        Venta v = new Venta();
        v.setFechaVenta(LocalDateTime.parse(fecha));
        v.setEstado(estado);
        v.setMontoTotal(new BigDecimal(monto));
        return v;
    }

    @Test
    void elAdminVeElValorDelInventarioYElEmpleadoNo() {
        when(usuarioActualService.esAdmin()).thenReturn(true);
        assertEquals(0, new BigDecimal("1500.50").compareTo(servicio.getDashboardStats().getInventarioStats().getValorTotalInventario()));

        when(usuarioActualService.esAdmin()).thenReturn(false);
        assertNull(servicio.getDashboardStats().getInventarioStats().getValorTotalInventario());
    }

    @Test
    void armaLasTarjetasConLasCifrasDeLasConsultas() {
        when(usuarioActualService.esAdmin()).thenReturn(true);

        DashboardResponse d = servicio.getDashboardStats();

        assertEquals(1L, d.getVentasStats().getTotalVentasHoy());
        assertEquals(10L, d.getVentasStats().getTotalVentasAño());
        assertEquals(12L, d.getProductosStats().getTotalProductos());
        assertEquals(2L, d.getProductosStats().getProductosSinStock());
        assertEquals(3L, d.getProductosStats().getProductosBajoStock());
        assertEquals(4L, d.getInventarioStats().getAlertasInventario());
        assertEquals(20L, d.getClientesStats().getTotalClientes());
        assertEquals(3L, d.getPagosStats().getCuotasPendientes());
        assertEquals(6L, d.getVentasPorEntregar());
        assertEquals(7, d.getVentasUltimosDias().size());
    }

    @Test
    void elPromedioDiarioEsElMontoDelMesEntreLosDiasTranscurridos() {
        when(usuarioActualService.esAdmin()).thenReturn(true);
        BigDecimal esperado = new BigDecimal("400.00")
                .divide(BigDecimal.valueOf(LocalDate.now().getDayOfMonth()), 2, RoundingMode.HALF_UP);

        assertEquals(0, esperado.compareTo(servicio.getDashboardStats().getVentasStats().getPromedioVentaDiaria()));
    }

    @Test
    void sinVentasEnElMesElPromedioEsCero() {
        ventasDelMes(0L, "0");
        when(usuarioActualService.esAdmin()).thenReturn(true);

        assertEquals(0, BigDecimal.ZERO.compareTo(servicio.getDashboardStats().getVentasStats().getPromedioVentaDiaria()));
    }

    @Test
    void laSemanaVaDeLunesADomingoYSoloCuentaLasCompletadas() {
        // 2026-10-21 es miércoles: la semana es del 19 al 25.
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(List.of(
                venta("2026-10-19T09:00:00", EstadoVenta.COMPLETADA, "100.00"),
                venta("2026-10-21T10:00:00", EstadoVenta.COMPLETADA, "250.00"),
                venta("2026-10-21T15:00:00", EstadoVenta.COMPLETADA, "50.00"),
                venta("2026-10-22T11:00:00", EstadoVenta.CANCELADA, "900.00"),
                venta("2026-10-23T11:00:00", EstadoVenta.PENDIENTE_PAGO, "700.00")));

        VentasSemanalResponse s = servicio.getVentasSemanal(LocalDate.of(2026, 10, 21));

        assertEquals("2026-10-19", s.getFechaInicio());
        assertEquals("2026-10-25", s.getFechaFin());
        assertEquals(7, s.getVentasPorDia().size());
        assertEquals(3L, s.getTotalVentas());
        assertEquals(0, new BigDecimal("400.00").compareTo(s.getMontoTotal()));
        DashboardResponse.VentaPorDiaDTO miercoles = s.getVentasPorDia().get(2);
        assertEquals("2026-10-21", miercoles.getFecha());
        assertEquals(2L, miercoles.getCantidadVentas());
        assertEquals(0, new BigDecimal("300.00").compareTo(miercoles.getMontoTotal()));
    }

    @Test
    void unDomingoPerteneceALaSemanaQueTerminaEseDia() {
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(Collections.emptyList());

        VentasSemanalResponse s = servicio.getVentasSemanal(LocalDate.of(2026, 10, 25));

        assertEquals("2026-10-19", s.getFechaInicio());
        assertEquals("2026-10-25", s.getFechaFin());
        assertEquals(0L, s.getTotalVentas());
        assertNotNull(s.getNumeroSemana());
        assertTrue(s.getVentasPorDia().stream().allMatch(d -> d.getCantidadVentas() == 0));
    }

    @Test
    void sinFechaSeUsaLaSemanaEnCurso() {
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(Collections.emptyList());

        assertTrue(servicio.getVentasSemanal(null).getEsSemanaActual());
    }

    @Test
    void losUltimosDiasSoloCuentanLasVentasCompletadas() {
        LocalDateTime hoy = LocalDate.now().atTime(12, 0);
        when(ventaRepository.findResumenVentasEntre(any(), any())).thenReturn(List.of(
                new Object[]{hoy, EstadoVenta.COMPLETADA, new BigDecimal("100.00")},
                new Object[]{hoy, EstadoVenta.CANCELADA, new BigDecimal("900.00")},
                new Object[]{hoy, EstadoVenta.PENDIENTE_PAGO, new BigDecimal("700.00")}));
        when(usuarioActualService.esAdmin()).thenReturn(true);

        List<DashboardResponse.VentaPorDiaDTO> dias = servicio.getDashboardStats().getVentasUltimosDias();

        DashboardResponse.VentaPorDiaDTO ultimo = dias.get(dias.size() - 1);
        assertEquals(LocalDate.now().toString(), ultimo.getFecha());
        assertEquals(1L, ultimo.getCantidadVentas());
        assertEquals(0, new BigDecimal("100.00").compareTo(ultimo.getMontoTotal()));
    }
}
