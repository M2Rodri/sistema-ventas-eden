package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ClienteConEstadisticasResponse;
import com.mitienda.ecommerce.dto.ClienteRequest;
import com.mitienda.ecommerce.dto.ClienteResponse;
import com.mitienda.ecommerce.dto.HistorialComprasResponse;
import com.mitienda.ecommerce.exception.ConflictoEstadoException;
import com.mitienda.ecommerce.exception.RecursoNoEncontradoException;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.ClienteRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de clientes: duplicados, búsqueda, historial de compras y estadísticas. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private RegistroAuditoria registroAuditoria;

    private ClienteService servicio;
    private Cliente ana;

    @BeforeEach
    void preparar() {
        servicio = new ClienteService(clienteRepository, ventaRepository, registroAuditoria);
        ana = cliente(1L, "Ana", "Rojas", "1234567", "70000001", "ana@correo.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            if (c.getId() == null) c.setId(99L);
            return c;
        });
    }

    private Cliente cliente(Long id, String nombre, String apellido, String nit, String telefono, String email) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombre(nombre);
        c.setApellido(apellido);
        c.setNitCi(nit);
        c.setTelefono(telefono);
        c.setEmail(email);
        c.setActivo(true);
        return c;
    }

    private ClienteRequest peticion(String nombre, String nit, String email) {
        ClienteRequest r = new ClienteRequest();
        r.setNombre(nombre);
        r.setApellido("Pérez");
        r.setNitCi(nit);
        r.setTelefono("70000002");
        r.setEmail(email);
        r.setActivo(true);
        return r;
    }

    private Venta venta(String fecha, String monto, String producto, int cantidad) {
        Producto p = new Producto();
        p.setNombre(producto);
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(cantidad);
        Venta v = new Venta();
        v.setEstado(EstadoVenta.COMPLETADA);
        v.setFechaVenta(LocalDateTime.parse(fecha));
        v.setMontoTotal(new BigDecimal(monto));
        v.setDetalles(new ArrayList<>(List.of(d)));
        v.setPagos(new ArrayList<>());
        return v;
    }

    // ---------- alta y edición ----------

    @Test
    void crearConDatosNuevosGuardaYAudita() {
        ClienteResponse r = servicio.createCliente(peticion("Luis", "9999999", "luis@correo.com"));

        assertEquals(99L, r.getId());
        verify(registroAuditoria).registrar(org.mockito.ArgumentMatchers.eq("CREAR_CLIENTE"),
                org.mockito.ArgumentMatchers.eq("clientes"), org.mockito.ArgumentMatchers.eq(99L), anyString());
    }

    @Test
    void crearConUnEmailYaUsadoEs409() {
        when(clienteRepository.existsByEmail("ana@correo.com")).thenReturn(true);

        ConflictoEstadoException e = assertThrows(ConflictoEstadoException.class,
                () -> servicio.createCliente(peticion("Otra", "111", "ana@correo.com")));

        assertEquals("EMAIL_DUPLICADO", e.getCodigo());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void crearConUnNitYaUsadoEs409() {
        when(clienteRepository.findByNitCi("1234567")).thenReturn(Optional.of(ana));

        ConflictoEstadoException e = assertThrows(ConflictoEstadoException.class,
                () -> servicio.createCliente(peticion("Otra", "1234567", null)));

        assertEquals("NIT_CI_DUPLICADO", e.getCodigo());
    }

    @Test
    void crearSinEmailNiNitNoValidaDuplicados() {
        servicio.createCliente(peticion("Mostrador", null, null));

        verify(clienteRepository, never()).existsByEmail(anyString());
        verify(clienteRepository, never()).findByNitCi(anyString());
    }

    @Test
    void editarConservandoSuPropioNitNoEsConflicto() {
        ClienteResponse r = servicio.updateCliente(1L, peticion("Ana María", "1234567", "ana@correo.com"));

        assertEquals("Ana María", r.getNombre());
    }

    @Test
    void editarConElNitDeOtroClienteEs409() {
        Cliente otro = cliente(2L, "Beto", "Díaz", "7654321", null, null);
        when(clienteRepository.findByNitCi("7654321")).thenReturn(Optional.of(otro));

        ConflictoEstadoException e = assertThrows(ConflictoEstadoException.class,
                () -> servicio.updateCliente(1L, peticion("Ana", "7654321", "ana@correo.com")));

        assertEquals("NIT_CI_DUPLICADO", e.getCodigo());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void editarConElEmailDeOtroClienteEs409() {
        when(clienteRepository.existsByEmail("beto@correo.com")).thenReturn(true);

        ConflictoEstadoException e = assertThrows(ConflictoEstadoException.class,
                () -> servicio.updateCliente(1L, peticion("Ana", "1234567", "beto@correo.com")));

        assertEquals("EMAIL_DUPLICADO", e.getCodigo());
    }

    @Test
    void unClienteInexistenteEs404() {
        RecursoNoEncontradoException e = assertThrows(RecursoNoEncontradoException.class, () -> servicio.getClienteById(777L));
        assertEquals("CLIENTE_NO_ENCONTRADO", e.getCodigo());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.updateCliente(777L, peticion("X", null, null)));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.getHistorialCompras(777L));
    }

    // ---------- búsqueda ----------

    @Test
    void laBusquedaSumaLosQueCoincidenPorTelefonoOPorNitSinRepetir() {
        Cliente porNombre = cliente(3L, "Ana", "Paz", null, null, null);
        Cliente porTelefono = cliente(4L, "Carlos", "Soto", null, "7777", null);
        when(clienteRepository.searchByNombre("7777")).thenReturn(new ArrayList<>(List.of(porNombre)));
        when(clienteRepository.findByTelefono("7777")).thenReturn(Optional.of(porTelefono));
        when(clienteRepository.findByNitCi("7777")).thenReturn(Optional.of(porTelefono));

        List<ClienteResponse> r = servicio.searchClientes("7777");

        assertEquals(2, r.size(), "el cliente hallado por teléfono y por NIT aparece una sola vez");
    }

    // ---------- historial de compras ----------

    @Test
    void sinComprasLasEstadisticasQuedanEnCero() {
        when(ventaRepository.findByClienteIdOrderByFechaVentaDesc(1L)).thenReturn(Collections.emptyList());

        HistorialComprasResponse h = servicio.getHistorialCompras(1L);

        assertEquals(0L, h.getEstadisticas().getTotalCompras());
        assertEquals(0, BigDecimal.ZERO.compareTo(h.getEstadisticas().getMontoTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(h.getEstadisticas().getTicketPromedio()));
        assertNull(h.getEstadisticas().getUltimaCompra());
        assertNull(h.getEstadisticas().getProductoMasComprado());
    }

    @Test
    void conComprasCalculaTotalTicketPromedioYProductoMasComprado() {
        Venta reciente = venta("2026-10-20T10:00:00", "300.00", "Cama", 1);
        Venta vieja = venta("2026-09-01T10:00:00", "500.00", "Colchón", 3);
        when(ventaRepository.findByClienteIdOrderByFechaVentaDesc(1L)).thenReturn(List.of(reciente, vieja));

        HistorialComprasResponse.EstadisticasCompra e = servicio.getHistorialCompras(1L).getEstadisticas();

        assertEquals(2L, e.getTotalCompras());
        assertEquals(0, new BigDecimal("800.00").compareTo(e.getMontoTotal()));
        assertEquals(0, new BigDecimal("400.00").compareTo(e.getTicketPromedio()));
        assertEquals(LocalDateTime.parse("2026-10-20T10:00:00"), e.getUltimaCompra());
        assertEquals("Colchón", e.getProductoMasComprado());
        assertEquals(3, e.getCantidadProductoMasComprado());
    }

    @Test
    void unaVentaCanceladaSeListaPeroNoCuentaEnLasEstadisticas() {
        Venta buena = venta("2026-09-01T10:00:00", "500.00", "Colchón", 1);
        Venta cancelada = venta("2026-10-20T10:00:00", "300.00", "Cama", 5);
        cancelada.setEstado(EstadoVenta.CANCELADA);
        when(ventaRepository.findByClienteIdOrderByFechaVentaDesc(1L)).thenReturn(List.of(cancelada, buena));

        HistorialComprasResponse h = servicio.getHistorialCompras(1L);

        assertEquals(2, h.getVentas().size());
        assertEquals(1L, h.getEstadisticas().getTotalCompras());
        assertEquals(0, new BigDecimal("500.00").compareTo(h.getEstadisticas().getMontoTotal()));
        assertEquals(LocalDateTime.parse("2026-09-01T10:00:00"), h.getEstadisticas().getUltimaCompra());
        assertEquals("Colchón", h.getEstadisticas().getProductoMasComprado());
    }

    @Test
    void elHistorialNoTrajoCostosNiGanancia() {
        when(ventaRepository.findByClienteIdOrderByFechaVentaDesc(1L))
                .thenReturn(List.of(venta("2026-10-20T10:00:00", "300.00", "Cama", 1)));

        servicio.getHistorialCompras(1L).getVentas().forEach(v -> {
            assertNull(v.getCostoTotal());
            assertNull(v.getGanancia());
        });
    }

    @Test
    void elHistorialFiltradoSoloIncluyeLasVentasDelRangoConLosExtremosIncluidos() {
        Venta dentro = venta("2026-10-10T08:00:00", "100.00", "Cama", 1);
        Venta enElBorde = venta("2026-10-01T00:00:00", "200.00", "Cama", 1);
        Venta fuera = venta("2026-08-15T08:00:00", "900.00", "Cama", 1);
        when(ventaRepository.findByClienteIdOrderByFechaVentaDesc(1L)).thenReturn(List.of(dentro, enElBorde, fuera));

        HistorialComprasResponse h = servicio.getHistorialComprasFiltrado(1L,
                LocalDateTime.parse("2026-10-01T00:00:00"), LocalDateTime.parse("2026-10-31T23:59:59"));

        assertEquals(2L, h.getEstadisticas().getTotalCompras());
        assertEquals(0, new BigDecimal("300.00").compareTo(h.getEstadisticas().getMontoTotal()));
    }

    // ---------- estadísticas en la tabla ----------

    @Test
    void lasEstadisticasDeLaTablaPonenCeroALosClientesSinCompras() {
        Cliente sinCompras = cliente(2L, "Beto", "Díaz", null, null, null);
        when(clienteRepository.findAll()).thenReturn(List.of(ana, sinCompras));
        Object[] filaAna = {1L, 4L, new BigDecimal("1200.00"), LocalDateTime.parse("2026-10-20T10:00:00")};
        when(ventaRepository.aggregarEstadisticasPorCliente()).thenReturn(Collections.singletonList(filaAna));

        List<ClienteConEstadisticasResponse> r = servicio.getAllClientesConEstadisticas();

        ClienteConEstadisticasResponse deAna = r.stream().filter(x -> x.getId().equals(1L)).findFirst().orElseThrow();
        ClienteConEstadisticasResponse deBeto = r.stream().filter(x -> x.getId().equals(2L)).findFirst().orElseThrow();
        assertEquals(4L, deAna.getNumeroCompras());
        assertEquals(0L, deBeto.getNumeroCompras());
        assertEquals(0, BigDecimal.ZERO.compareTo(deBeto.getMontoTotalComprado()));
        assertNull(deBeto.getUltimaFechaCompra());
        assertTrue(deAna.getMontoTotalComprado().compareTo(new BigDecimal("1200.00")) == 0);
    }
}
