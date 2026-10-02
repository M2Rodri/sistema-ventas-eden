package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CompraRequest;
import com.mitienda.ecommerce.dto.CompraResponse;
import com.mitienda.ecommerce.exception.ApiException;
import com.mitienda.ecommerce.models.Compra;
import com.mitienda.ecommerce.models.DetalleCompra;
import com.mitienda.ecommerce.models.EstadoCompra;
import com.mitienda.ecommerce.models.Inventario;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Proveedor;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.repositories.CompraRepository;
import com.mitienda.ecommerce.repositories.DetalleCompraRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.ProveedorRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Una compra es algo ya comprado: al registrarla entra al inventario y actualiza el costo; para
 * corregirla se anula (se descuenta lo que trajo, si todavía está en stock) y se registra de nuevo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private DetalleCompraRepository detalleCompraRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InventarioService inventarioService;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private RegistroAuditoria registroAuditoria;
    @Mock private UsuarioActualService usuarioActualService;

    private CompraService servicio;
    private Producto cama;
    private Inventario stock;

    @BeforeEach
    void preparar() {
        servicio = new CompraService(compraRepository, detalleCompraRepository, proveedorRepository,
                productoRepository, usuarioRepository, inventarioService, inventarioRepository,
                registroAuditoria, usuarioActualService);

        Usuario usuario = new Usuario();
        usuario.setId(3L);
        when(usuarioActualService.obtenerRequerido()).thenReturn(usuario);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(new Proveedor()));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> {
            Compra c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(10L);
            }
            return c;
        });

        cama = new Producto();
        cama.setId(7L);
        cama.setNombre("Cama");
        when(productoRepository.findById(7L)).thenReturn(Optional.of(cama));

        stock = new Inventario();
        stock.setCantidadDisponible(4);
        when(inventarioRepository.findByProductoId(7L)).thenReturn(Optional.of(stock));
    }

    private CompraRequest pedido(int cantidad, String precio) {
        CompraRequest.ItemCompraRequest item = new CompraRequest.ItemCompraRequest();
        item.setIdProducto(7L);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(new BigDecimal(precio));
        CompraRequest request = new CompraRequest();
        request.setIdProveedor(1L);
        request.setItems(List.of(item));
        return request;
    }

    private Proveedor proveedor() {
        Proveedor p = new Proveedor();
        p.setId(1L);
        p.setNombreEmpresa("Maderas");
        return p;
    }

    private Compra compraRegistrada(int cantidad) {
        Compra compra = new Compra();
        compra.setId(10L);
        compra.setEstado(EstadoCompra.CONFIRMADA);
        compra.setProveedor(proveedor());
        compra.setMontoTotal(new BigDecimal("100.00"));
        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(cama);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal("25.00"));
        compra.setDetalles(new ArrayList<>(List.of(detalle)));
        when(compraRepository.findById(10L)).thenReturn(Optional.of(compra));
        return compra;
    }

    @Test
    void registrarUnaCompra_laDejaConfirmada_sumaElStockYActualizaElCosto() {
        CompraResponse respuesta = servicio.createCompra(pedido(3, "25.00"));

        assertEquals(EstadoCompra.CONFIRMADA, respuesta.getEstado());
        verify(inventarioService).aumentarStock(7L, 3);
        verify(inventarioService).registrarAjusteAutomatico(eq(7L), eq(4), eq(7), eq("ENTRADA"), any(), eq(3L));
        assertEquals(new BigDecimal("25.00"), cama.getPrecioCompra());
        assertEquals(1, respuesta.getDetalles().size(), "la respuesta trae los productos de la compra");
    }

    @Test
    void registrarUnaCompraSinProveedor_funcionaYEntraAlStock() {
        CompraRequest pedido = pedido(3, "25.00");
        pedido.setIdProveedor(null);

        CompraResponse respuesta = servicio.createCompra(pedido);

        assertEquals(EstadoCompra.CONFIRMADA, respuesta.getEstado());
        assertNull(respuesta.getIdProveedor());
        assertNull(respuesta.getNombreProveedor());
        verify(inventarioService).aumentarStock(7L, 3);
    }

    @Test
    void anularUnaCompra_descuentaLoQueTrajoYLaMarcaAnulada() {
        stock.setCantidadDisponible(9);
        Compra compra = compraRegistrada(5);

        CompraResponse respuesta = servicio.cancelarCompra(10L);

        assertEquals(EstadoCompra.CANCELADA, respuesta.getEstado());
        assertEquals(EstadoCompra.CANCELADA, compra.getEstado());
        verify(inventarioService).reducirStock(7L, 5);
        verify(inventarioService).registrarAjusteAutomatico(eq(7L), eq(9), eq(4), eq("SALIDA"), any(), eq(3L));
    }

    @Test
    void anularUnaCompraCuyaMercaderiaYaSeVendio_es422YNoDescuentaNada() {
        stock.setCantidadDisponible(2);
        Compra compra = compraRegistrada(5);

        ApiException error = assertThrows(ApiException.class, () -> servicio.cancelarCompra(10L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getEstado());
        assertEquals("COMPRA_STOCK_VENDIDO", error.getCodigo());
        verify(inventarioService, never()).reducirStock(any(), any());
        assertEquals(EstadoCompra.CONFIRMADA, compra.getEstado());
    }
}
