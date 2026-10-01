package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.DatosEntregaRequest;
import com.mitienda.ecommerce.dto.VentaRequest;
import com.mitienda.ecommerce.dto.VentaResponse;
import com.mitienda.ecommerce.models.EstadoEntrega;
import com.mitienda.ecommerce.models.EstadoVenta;
import com.mitienda.ecommerce.models.MetodoPago;
import com.mitienda.ecommerce.models.ModalidadEntrega;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de entrega de una venta: quién puede pasar de un estado a otro, cómo
 * se retrocede, qué datos se piden según la modalidad y, sobre todo, que el
 * saldo pendiente NO bloquea ninguno de esos pasos.
 *
 * Son pruebas de la lógica del servicio con los repositorios simulados: no
 * tocan la base de datos ni levantan el servidor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceEntregaTest {

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
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioActualService.obtenerRequerido()).thenReturn(new Usuario());
    }

    private Venta venta(ModalidadEntrega modalidad, EstadoEntrega estado) {
        Venta v = new Venta();
        v.setModalidadEntrega(modalidad);
        v.setEstadoEntrega(estado);
        v.setEstado(EstadoVenta.COMPLETADA);
        v.setSaldoPendiente(BigDecimal.ZERO);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(v));
        return v;
    }

    private VentaRequest pedido(ModalidadEntrega modalidad, EstadoEntrega estado, String ciudad) {
        VentaRequest r = new VentaRequest();
        r.setNombreClienteInvitado("Cliente de prueba");
        r.setMetodoPago(MetodoPago.EFECTIVO);
        r.setItems(List.of());
        r.setModalidadEntrega(modalidad);
        r.setEstadoEntrega(estado);
        r.setCiudad(ciudad);
        return r;
    }

    // ---------- Entregar ----------

    @Test
    void entregarNoSeBloqueaPorSaldoPendiente() {
        Venta v = venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.PENDIENTE);
        v.setEstado(EstadoVenta.PENDIENTE_PAGO);
        v.setSaldoPendiente(new BigDecimal("500.00"));

        VentaResponse respuesta = servicio.marcarEntregado(1L);

        assertEquals(EstadoEntrega.ENTREGADO, respuesta.getEstadoEntrega());
    }

    @Test
    void entregarDirectoDesdePendienteEnTransportadora() {
        venta(ModalidadEntrega.TRANSPORTADORA, EstadoEntrega.PENDIENTE);
        assertEquals(EstadoEntrega.ENTREGADO, servicio.marcarEntregado(1L).getEstadoEntrega());
    }

    @Test
    void noSeEntregaUnaVentaCancelada() {
        Venta v = venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.PENDIENTE);
        v.setEstado(EstadoVenta.CANCELADA);
        assertThrows(RuntimeException.class, () -> servicio.marcarEntregado(1L));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void noSeEntregaDosVeces() {
        venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.ENTREGADO);
        assertThrows(RuntimeException.class, () -> servicio.marcarEntregado(1L));
    }

    // ---------- Corregir a pendiente ----------

    @Test
    void corregirEntregadoADomicilioVuelveAPendiente() {
        venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.ENTREGADO);
        assertEquals(EstadoEntrega.PENDIENTE, servicio.deshacerEntrega(1L).getEstadoEntrega());
    }

    @Test
    void corregirEntregadoEnTransportadoraVuelveAPendiente() {
        venta(ModalidadEntrega.TRANSPORTADORA, EstadoEntrega.ENTREGADO);
        assertEquals(EstadoEntrega.PENDIENTE, servicio.deshacerEntrega(1L).getEstadoEntrega());
    }

    @Test
    void corregirNoSeOfreceEnRetiro() {
        venta(ModalidadEntrega.RETIRO, EstadoEntrega.ENTREGADO);
        assertThrows(RuntimeException.class, () -> servicio.deshacerEntrega(1L));
    }

    @Test
    void corregirUnaVentaPendienteNoHaceNada() {
        venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.PENDIENTE);
        assertThrows(RuntimeException.class, () -> servicio.deshacerEntrega(1L));
    }

    @Test
    void corregirNoSeOfreceEnUnaVentaCancelada() {
        Venta v = venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.ENTREGADO);
        v.setEstado(EstadoVenta.CANCELADA);
        assertThrows(RuntimeException.class, () -> servicio.deshacerEntrega(1L));
    }

    @Test
    void corregirNoSeBloqueaPorSaldoPendiente() {
        Venta v = venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.ENTREGADO);
        v.setEstado(EstadoVenta.PENDIENTE_PAGO);
        v.setSaldoPendiente(new BigDecimal("200.00"));
        assertEquals(EstadoEntrega.PENDIENTE, servicio.deshacerEntrega(1L).getEstadoEntrega());
    }

    @Test
    void corregirQuedaEnLaAuditoria() {
        venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.ENTREGADO);
        servicio.deshacerEntrega(1L);
        verify(registroAuditoria).registrar(org.mockito.ArgumentMatchers.eq("DESHACER_ENTREGA"),
                any(), any(), any());
    }

    // ---------- Editar datos de entrega ----------

    @Test
    void editarDatosEnTransportadoraCambiaDireccionTransportadoraYGuia() {
        Venta v = venta(ModalidadEntrega.TRANSPORTADORA, EstadoEntrega.PENDIENTE);
        v.setCiudad("La Paz");

        servicio.actualizarDatosEntrega(1L,
                new DatosEntregaRequest("  Av. Siempre Viva 123 ", "Flota Bolivia", "  G-789 "));

        assertEquals("Av. Siempre Viva 123", v.getDireccionDestino());
        assertEquals("Flota Bolivia", v.getTransportadora());
        assertEquals("G-789", v.getGuiaRemision());
        assertEquals("La Paz", v.getCiudad());
        assertEquals(EstadoEntrega.PENDIENTE, v.getEstadoEntrega());
    }

    @Test
    void editarDatosEnDomicilioIgnoraTransportadoraYGuia() {
        Venta v = venta(ModalidadEntrega.DOMICILIO, EstadoEntrega.PENDIENTE);

        servicio.actualizarDatosEntrega(1L, new DatosEntregaRequest("Calle 1", "Flota X", "G-1"));

        assertEquals("Calle 1", v.getDireccionDestino());
        assertNull(v.getTransportadora());
        assertNull(v.getGuiaRemision());
    }

    @Test
    void editarDatosConValorVacioBorraElDato() {
        Venta v = venta(ModalidadEntrega.TRANSPORTADORA, EstadoEntrega.PENDIENTE);
        v.setGuiaRemision("G-1");

        servicio.actualizarDatosEntrega(1L, new DatosEntregaRequest("", "  ", ""));

        assertNull(v.getDireccionDestino());
        assertNull(v.getTransportadora());
        assertNull(v.getGuiaRemision());
    }

    @Test
    void editarDatosNoAplicaEnRetiro() {
        venta(ModalidadEntrega.RETIRO, EstadoEntrega.ENTREGADO);
        assertThrows(RuntimeException.class,
                () -> servicio.actualizarDatosEntrega(1L, new DatosEntregaRequest("x", null, null)));
    }

    // ---------- Registrar la venta ----------

    @Test
    void transportadoraExigeCiudad() {
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> servicio.createVentaDirecta(pedido(ModalidadEntrega.TRANSPORTADORA, null, "  ")));
        assertEquals("La ciudad es obligatoria para el envío por transportadora", error.getMessage());
    }
}
