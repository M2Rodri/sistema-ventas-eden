package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.exception.ApiException;
import com.mitienda.ecommerce.models.AlertaInventario;
import com.mitienda.ecommerce.models.EstadoAlerta;
import com.mitienda.ecommerce.models.Inventario;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.AlertaInventarioRepository;
import com.mitienda.ecommerce.repositories.InventarioRepository;
import com.mitienda.ecommerce.repositories.MovimientoInventarioRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * "Marcar como atendida": la alerta deja de avisar mientras el producto siga bajo el mínimo, y
 * vuelve a avisar si se repone y después baja otra vez.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertaAtendidaTest {

    @Mock private InventarioRepository inventarioRepository;
    @Mock private AlertaInventarioRepository alertaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RegistroAuditoria registroAuditoria;
    @Mock private UsuarioActualService usuarioActualService;

    private InventarioService servicio;
    private Producto cama;
    private Inventario inventario;

    @BeforeEach
    void preparar() {
        servicio = new InventarioService(inventarioRepository, alertaRepository, productoRepository,
                movimientoRepository, usuarioRepository, registroAuditoria, usuarioActualService);
        cama = new Producto();
        cama.setId(7L);
        cama.setSku("CAM-001");
        cama.setStockMinimo(3);
        inventario = new Inventario();
        inventario.setProducto(cama);
        inventario.setCantidadDisponible(2);
        when(inventarioRepository.findByProductoId(7L)).thenReturn(Optional.of(inventario));
    }

    private AlertaInventario alerta(EstadoAlerta estado) {
        AlertaInventario a = new AlertaInventario(cama, 2, 3);
        a.setId(1L);
        a.setEstado(estado);
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void marcarComoAtendida_pasaLaAlertaAAtendidaManual() {
        AlertaInventario alerta = alerta(EstadoAlerta.PENDIENTE);

        servicio.marcarAlertaAtendida(1L);

        assertEquals(EstadoAlerta.ATENDIDA_MANUAL, alerta.getEstado());
        verify(alertaRepository).save(alerta);
    }

    @Test
    void marcarUnaAlertaQueYaNoEstaPendiente_es409() {
        alerta(EstadoAlerta.ATENDIDA_MANUAL);

        ApiException error = assertThrows(ApiException.class, () -> servicio.marcarAlertaAtendida(1L));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        assertEquals("ALERTA_YA_ATENDIDA", error.getCodigo());
    }

    @Test
    void marcarUnaAlertaInexistente_es404() {
        when(alertaRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> servicio.marcarAlertaAtendida(99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("ALERTA_NO_ENCONTRADA", error.getCodigo());
    }

    @Test
    void conLaAlertaAtendida_unaVentaQueDejaElStockBajo_noCreaOtraAlerta() {
        when(alertaRepository.existsByProductoIdAndEstado(7L, EstadoAlerta.ATENDIDA_MANUAL)).thenReturn(true);

        servicio.sincronizarAlerta(7L);

        verify(alertaRepository, never()).save(any(AlertaInventario.class));
    }

    @Test
    void siSeRepone_laAlertaAtendidaSeCierra_yAlVolverABajarAvisaDeNuevo() {
        AlertaInventario atendida = new AlertaInventario(cama, 2, 3);
        atendida.setEstado(EstadoAlerta.ATENDIDA_MANUAL);
        when(alertaRepository.findByProductoIdAndEstado(7L, EstadoAlerta.PENDIENTE)).thenReturn(new ArrayList<>());
        when(alertaRepository.findByProductoIdAndEstado(7L, EstadoAlerta.ATENDIDA_MANUAL)).thenReturn(new ArrayList<>(List.of(atendida)));

        inventario.setCantidadDisponible(10); // se repuso por encima del mínimo
        servicio.sincronizarAlerta(7L);

        assertEquals(EstadoAlerta.ATENDIDA, atendida.getEstado());

        // Ya cerrada: al bajar otra vez no hay una atendida que la silencie, así que crea la alerta nueva.
        when(alertaRepository.existsByProductoIdAndEstado(7L, EstadoAlerta.ATENDIDA_MANUAL)).thenReturn(false);
        when(alertaRepository.existsByProductoIdAndEstado(7L, EstadoAlerta.PENDIENTE)).thenReturn(false);
        inventario.setCantidadDisponible(2);
        servicio.sincronizarAlerta(7L);

        verify(alertaRepository).save(any(AlertaInventario.class));
    }

    @Test
    void reactivarUnaAlertaAtendida_laVuelveAPendiente() {
        AlertaInventario atendida = new AlertaInventario(cama, 2, 3);
        atendida.setEstado(EstadoAlerta.ATENDIDA_MANUAL);
        when(alertaRepository.findByProductoIdAndEstado(7L, EstadoAlerta.ATENDIDA_MANUAL)).thenReturn(new ArrayList<>(List.of(atendida)));
        when(alertaRepository.existsByProductoIdAndEstado(7L, EstadoAlerta.PENDIENTE)).thenReturn(true);

        servicio.reactivarAlerta(7L);

        assertEquals(EstadoAlerta.PENDIENTE, atendida.getEstado());
    }

    @Test
    void reactivarUnProductoSinAlertaAtendida_es404() {
        when(alertaRepository.findByProductoIdAndEstado(7L, EstadoAlerta.ATENDIDA_MANUAL)).thenReturn(new ArrayList<>());

        ApiException error = assertThrows(ApiException.class, () -> servicio.reactivarAlerta(7L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
    }
}
