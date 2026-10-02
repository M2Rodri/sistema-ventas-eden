package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Producto;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Ventas por producto: una fila por producto de cada venta del período, sin las canceladas. */
@ExtendWith(MockitoExtension.class)
class ReporteVentasPorProductoTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private CompraRepository compraRepository;

    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(ventaRepository, detalleVentaRepository, inventarioRepository, compraRepository);
    }

    private Venta venta(long id, EstadoVenta estado, String cliente, Producto producto, int cantidad, String precio) {
        Cliente c = new Cliente();
        c.setNombre(cliente);
        Venta venta = new Venta();
        venta.setId(id);
        venta.setEstado(estado);
        venta.setCliente(c);
        venta.setFechaVenta(LocalDateTime.of(2026, 9, 20, 10, 0));
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal(precio));
        detalle.setSubtotal(new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        venta.setDetalles(new ArrayList<>(List.of(detalle)));
        return venta;
    }

    @Test
    void devuelveUnaFilaPorProductoVendido_yDejaFueraLasVentasCanceladas() {
        Categoria camas = new Categoria();
        camas.setNombre("Camas");
        Producto cama = new Producto();
        cama.setId(7L);
        cama.setNombre("Cama Matrimonial");
        cama.setSku("CAM-001");
        cama.setCategoria(camas);

        when(ventaRepository.findByFechaVentaBetweenOrderByFechaVentaDesc(any(), any())).thenReturn(List.of(
                venta(2, EstadoVenta.COMPLETADA, "Ana", cama, 2, "500.00"),
                venta(1, EstadoVenta.CANCELADA, "Luis", cama, 1, "500.00")));

        List<Map<String, Object>> filas = servicio.getReporteVentasPorProducto(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59));

        assertEquals(1, filas.size());
        Map<String, Object> fila = filas.get(0);
        assertEquals(2L, fila.get("idVenta"));
        assertEquals("CAM-001", fila.get("skuProducto"));
        assertEquals("Camas", fila.get("categoria"));
        assertEquals(2, fila.get("cantidad"));
        assertEquals(0, new BigDecimal("1000.00").compareTo((BigDecimal) fila.get("subtotal")));
    }
}
