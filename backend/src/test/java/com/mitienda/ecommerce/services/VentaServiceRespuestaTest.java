package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.DetalleVenta;
import com.mitienda.ecommerce.models.Inventario;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.Pago;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * La respuesta de registrar una venta tiene que incluir el pago y los productos
 * recién guardados.
 *
 * La web sube la foto del comprobante a pagos[0] de esa respuesta apenas se
 * registra la venta. Antes la respuesta salía con la lista de pagos vacía y la
 * foto nunca se subía, sin ningún aviso.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceRespuestaTest {

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

    @BeforeEach
    void preparar() {
        servicio = new VentaService(ventaRepository, detalleVentaRepository, pagoRepository,
                clienteRepository, productoRepository, usuarioRepository, inventarioService,
                inventarioRepository, registroAuditoria, usuarioActualService);

        Usuario vendedor = new Usuario();
        vendedor.setId(1L);
        when(usuarioActualService.obtenerRequerido()).thenReturn(vendedor);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente cliente = i.getArgument(0);
            cliente.setId(5L);
            return cliente;
        });

        Producto producto = new Producto();
        producto.setId(7L);
        producto.setNombre("Almohada");
        producto.setActivo(true);
        producto.setPrecioVenta(new BigDecimal("135.00"));
        when(productoRepository.findById(7L)).thenReturn(Optional.of(producto));
        when(inventarioService.verificarDisponibilidad(anyLong(), anyInt())).thenReturn(true);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setCantidadDisponible(10);
        when(inventarioRepository.findByProductoId(7L)).thenReturn(Optional.of(inventario));

        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta venta = i.getArgument(0);
            venta.setId(40L);
            return venta;
        });
        when(detalleVentaRepository.save(any(DetalleVenta.class))).thenAnswer(i -> i.getArgument(0));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> {
            Pago pago = i.getArgument(0);
            pago.setId(77L);
            return pago;
        });
    }

    private VentaRequest pedido(MetodoPago metodo) {
        VentaRequest.ItemVentaRequest item = new VentaRequest.ItemVentaRequest();
        item.setIdProducto(7L);
        item.setCantidad(1);

        VentaRequest pedido = new VentaRequest();
        pedido.setNombreClienteInvitado("Cliente de prueba");
        pedido.setMetodoPago(metodo);
        pedido.setItems(List.of(item));
        return pedido;
    }

    @Test
    void laRespuestaIncluyeElPagoRecienGuardadoConSuId() {
        VentaResponse respuesta = servicio.createVentaDirecta(pedido(MetodoPago.QR));

        assertEquals(1, respuesta.getPagos().size());
        assertEquals(77L, respuesta.getPagos().get(0).getId());
        assertEquals(MetodoPago.QR, respuesta.getPagos().get(0).getMetodoPago());
        assertEquals(0, new BigDecimal("135.00").compareTo(respuesta.getPagos().get(0).getMonto()));
    }

    @Test
    void unPagoPorQrSinFotoSeMarcaSinRespaldoEnLaRespuesta() {
        VentaResponse respuesta = servicio.createVentaDirecta(pedido(MetodoPago.QR));

        assertTrue(respuesta.isTienePagosSinRespaldo());
    }

    @Test
    void laRespuestaIncluyeLosProductosVendidos() {
        VentaResponse respuesta = servicio.createVentaDirecta(pedido(MetodoPago.EFECTIVO));

        assertNotNull(respuesta.getDetalles());
        assertEquals(1, respuesta.getDetalles().size());
    }
}
