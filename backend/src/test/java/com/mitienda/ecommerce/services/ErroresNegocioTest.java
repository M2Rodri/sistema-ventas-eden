package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.MovimientoInventarioRequest;
import com.mitienda.ecommerce.dto.PagoRequest;
import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.exception.ApiException;
import com.mitienda.ecommerce.models.Compra;
import com.mitienda.ecommerce.models.EstadoCompra;
import com.mitienda.ecommerce.models.EstadoEntrega;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.Inventario;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.ModalidadEntrega;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Usuario;
import com.mitienda.ecommerce.models.Venta;
import com.mitienda.ecommerce.repositories.AlertaInventarioRepository;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.CompraRepository;
import com.mitienda.ecommerce.repositories.DetalleCompraRepository;
import com.mitienda.ecommerce.repositories.DetalleVentaRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.MovimientoInventarioRepository;
import com.mitienda.ecommerce.repositories.PagoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.ProveedorRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Cada regla de negocio sale con su código HTTP y su código de error: recurso
 * inexistente (404), conflicto con el estado actual (409), regla de negocio (422)
 * y dato inválido (400). El formato JSON de la respuesta lo cubre FormatoErrorApiTest.
 *
 * Son pruebas de la lógica de los servicios con los repositorios simulados.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErroresNegocioTest {

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
    @Mock private CompraRepository compraRepository;
    @Mock private DetalleCompraRepository detalleCompraRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private AlmacenArchivos almacen;
    @Mock private AlertaInventarioRepository alertaInventarioRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;

    private VentaService ventas;
    private PagoService pagos;
    private CompraService compras;
    private InventarioService inventario;

    @BeforeEach
    void preparar() {
        ventas = new VentaService(ventaRepository, detalleVentaRepository, pagoRepository,
                clienteRepository, productoRepository, usuarioRepository, inventarioService,
                inventarioRepository, registroAuditoria, usuarioActualService);
        pagos = new PagoService(pagoRepository, ventaRepository, almacen, usuarioActualService);
        compras = new CompraService(compraRepository, detalleCompraRepository, proveedorRepository,
                productoRepository, usuarioRepository, inventarioService, inventarioRepository,
                registroAuditoria, usuarioActualService);
        inventario = new InventarioService(inventarioRepository, alertaInventarioRepository, productoRepository,
                movimientoInventarioRepository, usuarioRepository, registroAuditoria, usuarioActualService);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioActualService.obtenerRequerido()).thenReturn(new Usuario());
    }

    private void esperar(HttpStatus estado, String codigo, Runnable accion) {
        ApiException error = assertThrows(ApiException.class, accion::run);
        assertEquals(estado, error.getEstado(), "código HTTP de " + codigo);
        assertEquals(codigo, error.getCodigo());
    }

    private Venta ventaGuardada(EstadoVenta estado) {
        Venta v = new Venta();
        v.setEstado(estado);
        v.setModalidadEntrega(ModalidadEntrega.DOMICILIO);
        v.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        v.setSaldoPendiente(new BigDecimal("100.00"));
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(v));
        return v;
    }

    private VentaRequest ventaDe(Long idProducto, int cantidad, String precio) {
        VentaRequest.ItemVentaRequest item = new VentaRequest.ItemVentaRequest();
        item.setIdProducto(idProducto);
        item.setCantidad(cantidad);
        if (precio != null) {
            item.setPrecioUnitarioConDescuento(new BigDecimal(precio));
        }
        VentaRequest request = new VentaRequest();
        request.setNombreClienteInvitado("Cliente de mostrador");
        request.setItems(List.of(item));
        request.setMontoPagado(BigDecimal.ZERO);
        return request;
    }

    private Producto productoActivo() {
        Producto p = new Producto();
        p.setId(7L);
        p.setNombre("Cama");
        p.setActivo(true);
        p.setPrecioVenta(new BigDecimal("500.00"));
        when(productoRepository.findById(7L)).thenReturn(Optional.of(p));
        when(inventarioService.verificarDisponibilidad(anyLong(), anyInt())).thenReturn(true);
        return p;
    }

    // ---------- 404: el recurso no existe (también al modificar) ----------

    @Test
    void ventaInexistente_es404_alLeerCancelarYEntregar() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());
        esperar(HttpStatus.NOT_FOUND, "VENTA_NO_ENCONTRADA", () -> ventas.getVentaById(99L));
        esperar(HttpStatus.NOT_FOUND, "VENTA_NO_ENCONTRADA", () -> ventas.cancelarVenta(99L));
        esperar(HttpStatus.NOT_FOUND, "VENTA_NO_ENCONTRADA", () -> ventas.marcarEntregado(99L));
    }

    @Test
    void productoInexistenteEnUnaVenta_es404() {
        when(productoRepository.findById(5L)).thenReturn(Optional.empty());
        esperar(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", () -> ventas.createVentaDirecta(ventaDe(5L, 1, null)));
    }

    @Test
    void compraInexistente_es404_alAnular() {
        when(compraRepository.findById(99L)).thenReturn(Optional.empty());
        esperar(HttpStatus.NOT_FOUND, "COMPRA_NO_ENCONTRADA", () -> compras.cancelarCompra(99L));
    }

    @Test
    void ventaInexistenteAlRegistrarUnPago_es404() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());
        PagoRequest pago = new PagoRequest();
        pago.setIdVenta(99L);
        pago.setMonto(BigDecimal.TEN);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        esperar(HttpStatus.NOT_FOUND, "VENTA_NO_ENCONTRADA", () -> pagos.registrarPago(pago));
    }

    // ---------- 409: choca con el estado actual ----------

    @Test
    void cancelarUnaVentaYaCancelada_es409() {
        ventaGuardada(EstadoVenta.CANCELADA);
        esperar(HttpStatus.CONFLICT, "VENTA_YA_CANCELADA", () -> ventas.cancelarVenta(1L));
    }

    @Test
    void entregarUnaVentaCancelada_o_yaEntregada_es409() {
        Venta venta = ventaGuardada(EstadoVenta.CANCELADA);
        esperar(HttpStatus.CONFLICT, "VENTA_CANCELADA", () -> ventas.marcarEntregado(1L));

        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setEstadoEntrega(EstadoEntrega.ENTREGADO);
        esperar(HttpStatus.CONFLICT, "VENTA_YA_ENTREGADA", () -> ventas.marcarEntregado(1L));
    }

    @Test
    void pagoSobreUnaVentaCancelada_es409() {
        ventaGuardada(EstadoVenta.CANCELADA);
        PagoRequest pago = new PagoRequest();
        pago.setIdVenta(1L);
        pago.setMonto(BigDecimal.TEN);
        esperar(HttpStatus.CONFLICT, "VENTA_CANCELADA", () -> pagos.registrarPago(pago));
    }

    @Test
    void anularUnaCompraYaAnulada_es409() {
        Compra compra = new Compra();
        compra.setEstado(EstadoCompra.CANCELADA);
        when(compraRepository.findById(1L)).thenReturn(Optional.of(compra));
        esperar(HttpStatus.CONFLICT, "COMPRA_YA_ANULADA", () -> compras.cancelarCompra(1L));
    }

    // ---------- 422: regla de negocio ----------

    @Test
    void pagoMayorAlSaldo_es422() {
        ventaGuardada(EstadoVenta.COMPLETADA);
        PagoRequest pago = new PagoRequest();
        pago.setIdVenta(1L);
        pago.setMonto(new BigDecimal("100.01"));
        esperar(HttpStatus.UNPROCESSABLE_ENTITY, "PAGO_EXCEDE_SALDO", () -> pagos.registrarPago(pago));
    }

    @Test
    void stockInsuficiente_es422() {
        productoActivo();
        when(inventarioService.verificarDisponibilidad(7L, 3)).thenReturn(false);
        esperar(HttpStatus.UNPROCESSABLE_ENTITY, "STOCK_INSUFICIENTE", () -> ventas.createVentaDirecta(ventaDe(7L, 3, null)));
    }

    @Test
    void precioMayorAlDeCatalogo_es422() {
        productoActivo();
        esperar(HttpStatus.UNPROCESSABLE_ENTITY, "PRECIO_EXCEDE_CATALOGO",
                () -> ventas.createVentaDirecta(ventaDe(7L, 1, "500.01")));
    }

    @Test
    void productoDeBaja_es422() {
        productoActivo().setActivo(false);
        esperar(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_NO_DISPONIBLE", () -> ventas.createVentaDirecta(ventaDe(7L, 1, null)));
    }

    @Test
    void ajusteDeSalidaMayorAlStock_es422_conElMensaje() {
        Inventario stock = new Inventario();
        stock.setCantidadDisponible(5);
        when(inventarioRepository.findByProductoId(7L)).thenReturn(Optional.of(stock));
        MovimientoInventarioRequest request = new MovimientoInventarioRequest();
        request.setIdProducto(7L);
        request.setCantidad(6);
        request.setTipoMovimiento("SALIDA");

        ApiException error = assertThrows(ApiException.class, () -> inventario.ajustarInventario(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getEstado());
        assertEquals("STOCK_INSUFICIENTE", error.getCodigo());
        assertEquals("Stock insuficiente. Disponible: 5", error.getMessage());
        assertEquals(5, stock.getCantidadDisponible(), "no descuenta nada");
    }

    // ---------- 400: dato inválido ----------

    @Test
    void ventaSinCliente_es400() {
        VentaRequest request = ventaDe(7L, 1, null);
        request.setNombreClienteInvitado(null);
        esperar(HttpStatus.BAD_REQUEST, "CLIENTE_REQUERIDO", () -> ventas.createVentaDirecta(request));
    }
}
