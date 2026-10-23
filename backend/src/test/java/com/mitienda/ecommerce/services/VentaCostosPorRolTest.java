package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.DetalleVentaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.PagoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** El costo y la ganancia de una venta solo le llegan al Admin. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaCostosPorRolTest {
    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InventarioService inventarioService;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private RegistroAuditoria registroAuditoria;
    @Mock private UsuarioActualService usuarioActualService;

    private VentaService servicio;
    private Venta venta;

    @BeforeEach
    void preparar() {
        servicio = new VentaService(ventaRepository, detalleVentaRepository, pagoRepository,
                clienteRepository, productoRepository, usuarioRepository, inventarioService,
                inventarioRepository, registroAuditoria, usuarioActualService);

        Producto cama = new Producto();
        cama.setId(5L);
        cama.setNombre("Cama 2 plazas");
        cama.setSku("CAM-0001");

        DetalleVenta linea = new DetalleVenta();
        linea.setId(1L);
        linea.setProducto(cama);
        linea.setCantidad(2);
        linea.setPrecioUnitario(new BigDecimal("500.00"));
        linea.setCostoUnitario(new BigDecimal("300.00"));
        linea.setSubtotal(new BigDecimal("1000.00"));

        Usuario vendedor = new Usuario();
        vendedor.setId(1L);
        vendedor.setNombre("Ana");
        vendedor.setApellido("Pérez");

        venta = new Venta();
        venta.setId(10L);
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setUsuario(vendedor);
        venta.setSubtotal(new BigDecimal("1000.00"));
        venta.setMontoTotal(new BigDecimal("1000.00"));
        venta.setSaldoPendiente(BigDecimal.ZERO);
        venta.setDetalles(new ArrayList<>(List.of(linea)));
        venta.setPagos(new ArrayList<>());
        venta.setFechaVenta(LocalDateTime.of(2026, 10, 20, 10, 0));

        when(ventaRepository.findById(10L)).thenReturn(Optional.of(venta));
        when(ventaRepository.findAll(any(Sort.class))).thenReturn(List.of(venta));
        when(ventaRepository.findByClienteIdOrderByFechaVentaDesc(anyLong())).thenReturn(List.of(venta));
        when(ventaRepository.findByEstadoOrderByFechaVentaDesc(any())).thenReturn(List.of(venta));
        when(ventaRepository.findVentasDelDia()).thenReturn(List.of(venta));
        when(ventaRepository.findTop10ByOrderByFechaVentaDesc()).thenReturn(List.of(venta));
        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(List.of(venta));
    }

    private void comoAdmin(boolean esAdmin) {
        when(usuarioActualService.esAdmin()).thenReturn(esAdmin);
    }

    private void assertSinCostos(VentaResponse respuesta) {
        assertNull(respuesta.getCostoTotal(), "el costo total no debe llegar");
        assertNull(respuesta.getGanancia(), "la ganancia no debe llegar");
        assertFalse(respuesta.getDetalles().isEmpty());
        respuesta.getDetalles().forEach(d -> assertNull(d.getCostoUnitario(), "el costo unitario no debe llegar"));
    }

    private void assertConCostos(VentaResponse respuesta) {
        assertEquals(0, new BigDecimal("600.00").compareTo(respuesta.getCostoTotal()), "costo = 2 x 300");
        assertEquals(0, new BigDecimal("400.00").compareTo(respuesta.getGanancia()), "ganancia = 1000 - 600");
        respuesta.getDetalles().forEach(d -> assertNotNull(d.getCostoUnitario()));
    }

    // ---------- la respuesta por sí sola ----------

    @Test
    void laRespuestaPorDefectoNoTraeCostos() {
        assertSinCostos(new VentaResponse(venta));
    }

    @Test
    void conIncluirCostosEnFalsoTampocoTraeCostos() {
        assertSinCostos(new VentaResponse(venta, false));
    }

    @Test
    void conIncluirCostosEnVerdaderoTraeCostoYGanancia() {
        assertConCostos(new VentaResponse(venta, true));
    }

    // ---------- el servicio, según el rol ----------

    @Test
    void elEmpleadoNoRecibeCostosEnNingunaConsultaDeVentas() {
        comoAdmin(false);

        assertSinCostos(servicio.getVentaById(10L));
        servicio.getAllVentas().forEach(this::assertSinCostos);
        servicio.getVentasByCliente(3L).forEach(this::assertSinCostos);
        servicio.getVentasByEstado(EstadoVenta.COMPLETADA).forEach(this::assertSinCostos);
        servicio.getVentasDelDia().forEach(this::assertSinCostos);
        servicio.getUltimasVentas().forEach(this::assertSinCostos);
        servicio.getVentasByFechas(LocalDateTime.MIN, LocalDateTime.MAX).forEach(this::assertSinCostos);
    }

    @Test
    void elAdminRecibeCostosEnTodasLasConsultasDeVentas() {
        comoAdmin(true);

        assertConCostos(servicio.getVentaById(10L));
        servicio.getAllVentas().forEach(this::assertConCostos);
        servicio.getVentasByCliente(3L).forEach(this::assertConCostos);
        servicio.getVentasByEstado(EstadoVenta.COMPLETADA).forEach(this::assertConCostos);
        servicio.getVentasDelDia().forEach(this::assertConCostos);
        servicio.getUltimasVentas().forEach(this::assertConCostos);
        servicio.getVentasByFechas(LocalDateTime.MIN, LocalDateTime.MAX).forEach(this::assertConCostos);
    }

    @Test
    void sinUsuarioIdentificadoSeTrataComoEmpleado() {
        // esAdmin() devuelve false si no hay usuario: ante la duda, no se entregan costos.
        when(usuarioActualService.esAdmin()).thenReturn(false);

        assertSinCostos(servicio.getVentaById(10L));
    }

    @Test
    void unaVentaCanceladaNoTraeGananciaAunqueSeaAdmin() {
        comoAdmin(true);
        venta.setEstado(EstadoVenta.CANCELADA);

        VentaResponse respuesta = servicio.getVentaById(10L);

        assertNull(respuesta.getGanancia());
        assertNull(respuesta.getCostoTotal());
    }
}
