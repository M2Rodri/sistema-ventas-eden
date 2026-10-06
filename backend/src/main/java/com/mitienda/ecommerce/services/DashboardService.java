package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.DashboardResponse;
import com.mitienda.ecommerce.dto.VentasSemanalResponse;
import com.mitienda.ecommerce.models.*;
import com.mitienda.ecommerce.repositories.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final VentaRepository ventaRepository;

    private final ProductoRepository productoRepository;

    private final InventarioRepository inventarioRepository;

    private final ClienteRepository clienteRepository;

    private final AlertaInventarioRepository alertaInventarioRepository;

    private final DetalleVentaRepository detalleVentaRepository;

    private final UsuarioActualService usuarioActualService;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public DashboardService(VentaRepository ventaRepository,
                            ProductoRepository productoRepository,
                            InventarioRepository inventarioRepository,
                            ClienteRepository clienteRepository,
                            AlertaInventarioRepository alertaInventarioRepository,
                            DetalleVentaRepository detalleVentaRepository,
                            UsuarioActualService usuarioActualService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.clienteRepository = clienteRepository;
        this.alertaInventarioRepository = alertaInventarioRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.usuarioActualService = usuarioActualService;
    }


    /**
     * Estadísticas del panel de inicio.
     *
     * Requiere transacción: getProductosMasVendidos() agrupa por el Producto de
     * cada DetalleVenta, que es una relación perezosa. Con
     * spring.jpa.open-in-view=false no hay sesión abierta fuera de la
     * transacción, y sin esta anotación el endpoint fallaba entero con
     * LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats() {
        DashboardResponse dashboard = new DashboardResponse();

        // Una consulta por tema: cada ida y vuelta a la base pesa en
        // producción, así que se piden las cifras agrupadas.
        Object[] inventario = inventarioRepository.resumenInventario().get(0);
        dashboard.setVentasStats(getVentasStats());
        dashboard.setProductosStats(getProductosStats(inventario));
        dashboard.setInventarioStats(getInventarioStats(inventario));
        dashboard.setClientesStats(getClientesStats());
        dashboard.setPagosStats(getPagosStats());
        dashboard.setProductosMasVendidos(getProductosMasVendidos(10));
        dashboard.setVentasUltimosDias(getVentasUltimosDias(7));
        dashboard.setVentasPorEntregar(ventaRepository.countVentasPorEntregar());

        return dashboard;
    }

    private DashboardResponse.VentasStats getVentasStats() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioAño = LocalDate.now().withDayOfYear(1).atStartOfDay();

        // El conteo cuenta las mismas ventas que suma el monto (COMPLETADA):
        // antes el conteo incluía todos los estados y el monto solo las
        // completadas, y quedaba "3 ventas registradas" al lado de un monto
        // que en realidad era la suma de 2 — parecía que faltaba plata.
        Object[] fila = ventaRepository.resumenVentasCompletadas(hoy, finHoy, inicioMes, inicioAño).get(0);
        Long totalVentasHoy = aLong(fila[0]);
        BigDecimal montoVentasHoy = aDecimal(fila[1]);
        Long totalVentasMes = aLong(fila[2]);
        BigDecimal montoVentasMes = aDecimal(fila[3]);
        Long totalVentasAño = aLong(fila[4]);
        BigDecimal montoVentasAño = aDecimal(fila[5]);

        BigDecimal promedioVentaDiaria = totalVentasMes > 0
                ? montoVentasMes.divide(BigDecimal.valueOf(LocalDate.now().getDayOfMonth()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new DashboardResponse.VentasStats(
                totalVentasHoy, montoVentasHoy,
                totalVentasMes, montoVentasMes,
                totalVentasAño, montoVentasAño,
                promedioVentaDiaria
        );
    }

    private DashboardResponse.ProductosStats getProductosStats(Object[] inventario) {
        Object[] conteo = productoRepository.resumenConteo().get(0);
        return new DashboardResponse.ProductosStats(
                aLong(conteo[0]), aLong(conteo[1]),
                aLong(inventario[0]), aLong(inventario[1])
        );
    }

    private DashboardResponse.InventarioStats getInventarioStats(Object[] inventario) {
        Long alertasInventario = alertaInventarioRepository.countByEstado(EstadoAlerta.PENDIENTE);

        // Se calcula con precioCompra: el rol EMPLEADO no debe verlo, asi
        // que para EMPLEADO (o sin sesion) esta tarjeta queda en null.
        BigDecimal valorTotal = usuarioActualService.esAdmin() ? aDecimal(inventario[2]) : null;

        Integer ajustesDelMes = 0;

        return new DashboardResponse.InventarioStats(
                alertasInventario, valorTotal, ajustesDelMes
        );
    }

    private DashboardResponse.ClientesStats getClientesStats() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Object[] fila = clienteRepository.resumenClientes(hoy, finHoy, inicioMes).get(0);
        return new DashboardResponse.ClientesStats(
                aLong(fila[0]), aLong(fila[2]),
                aLong(fila[3]), aLong(fila[1])
        );
    }

    private static Long aLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private static BigDecimal aDecimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    /**
     * "Cuotas" es el nombre historico del campo, pero en 'ventas' no hay tabla
     * de cuotas: lo que hay es saldoPendiente por venta. Por cobrar = ventas
     * con saldo o que nunca se marcaron como completadas.
     *
     * No hay fecha de vencimiento en 'ventas', asi que no existe "vencidas".
     */
    private DashboardResponse.PagosStats getPagosStats() {
        Object[] fila = ventaRepository.resumenPorCobrar().get(0);
        return new DashboardResponse.PagosStats(aLong(fila[0]), aDecimal(fila[1]));
    }

    /**
     * Productos mas vendidos de toda la historia del negocio.
     *
     * Antes este metodo traia la tabla detalle_venta completa con findAll() y
     * agrupaba en memoria, sin filtrar por estado: las ventas canceladas
     * seguian sumando al ranking. Ahora agrupa la base de datos y solo cuenta
     * las ventas COMPLETADAS.
     */
    private List<DashboardResponse.ProductoMasVendidoDTO> getProductosMasVendidos(int limite) {
        // Desde una fecha bien anterior a cualquier venta posible: equivale a
        // "sin limite inferior", sin necesitar una segunda consulta.
        return getMasVendidosEntre(LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now().plusDays(1), limite);
    }

    /**
     * Productos mas vendidos dentro de un rango.
     *
     * Se expone aparte porque el aviso de cierre del dia necesita responder
     * "que se vendio hoy", y el metodo anterior solo sabia de toda la historia.
     *
     * @param desde  inclusive
     * @param hasta  exclusive
     */
    public List<DashboardResponse.ProductoMasVendidoDTO> getMasVendidosEntre(
            LocalDateTime desde, LocalDateTime hasta, int limite) {

        Pageable tope = PageRequest.of(0, limite);

        return detalleVentaRepository.findMasVendidosEntre(desde, hasta, tope)
                .stream()
                .map(fila -> new DashboardResponse.ProductoMasVendidoDTO(
                        (Long) fila[0],
                        (String) fila[1],
                        (String) fila[2],
                        // SUM sobre un Integer devuelve Long en JPQL; SUM sobre
                        // un BigDecimal devuelve BigDecimal.
                        ((Number) fila[3]).longValue(),
                        (BigDecimal) fila[4]
                ))
                .collect(Collectors.toList());
    }

    /**
     * Lo que se vendio hoy, para el aviso de cierre del dia.
     *
     * El dia va de las 00:00 de hoy a las 00:00 de manana, sin incluir ese
     * limite: asi una venta registrada a las 23:59:59 entra en el dia correcto.
     */
    public List<DashboardResponse.ProductoMasVendidoDTO> getMasVendidosHoy(int limite) {
        LocalDateTime inicioDelDia = LocalDate.now().atStartOfDay();
        return getMasVendidosEntre(inicioDelDia, inicioDelDia.plusDays(1), limite);
    }

    /**
     * Ventas completadas de la semana calendario (lunes a domingo) que contiene
     * a la fecha dada; sin fecha, la semana en curso. El lunes a las 00:00
     * arranca en cero y el domingo a las 23:59 cierra: no es una ventana móvil.
     */
    @Transactional(readOnly = true)
    public VentasSemanalResponse getVentasSemanal(LocalDate fecha) {
        LocalDate referencia = fecha != null ? fecha : LocalDate.now();
        LocalDate lunes = referencia.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);

        List<Venta> ventasSemana = ventaRepository
                .findByFechaVentaBetweenOrderByFechaVentaDesc(lunes.atStartOfDay(), domingo.plusDays(1).atStartOfDay())
                .stream().filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).toList();

        List<DashboardResponse.VentaPorDiaDTO> porDia = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            List<Venta> ventasDia = ventasSemana.stream()
                    .filter(v -> v.getFechaVenta().toLocalDate().equals(dia))
                    .toList();
            porDia.add(new DashboardResponse.VentaPorDiaDTO(
                    dia.format(formatter),
                    (long) ventasDia.size(),
                    ventasDia.stream().map(Venta::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
            ));
        }

        BigDecimal montoTotal = ventasSemana.stream().map(Venta::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate hoy = LocalDate.now();

        return new VentasSemanalResponse(
                lunes.format(formatter),
                domingo.format(formatter),
                lunes.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                !hoy.isBefore(lunes) && !hoy.isAfter(domingo),
                (long) ventasSemana.size(),
                montoTotal,
                porDia
        );
    }

    private List<DashboardResponse.VentaPorDiaDTO> getVentasUltimosDias(int dias) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate primerDia = LocalDate.now().minusDays(dias - 1L);

        // Una sola consulta para todos los días (antes era una por día).
        List<Object[]> ventas = ventaRepository.findResumenVentasEntre(
                primerDia.atStartOfDay(), LocalDate.now().atTime(23, 59, 59));

        List<DashboardResponse.VentaPorDiaDTO> ventasPorDia = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = primerDia.plusDays(i);
            long cantidadVentas = 0;
            BigDecimal montoTotal = BigDecimal.ZERO;
            for (Object[] v : ventas) {
                if (((LocalDateTime) v[0]).toLocalDate().equals(fecha)) {
                    cantidadVentas++;
                    if (v[1] == EstadoVenta.COMPLETADA) {
                        montoTotal = montoTotal.add((BigDecimal) v[2]);
                    }
                }
            }
            ventasPorDia.add(new DashboardResponse.VentaPorDiaDTO(
                    fecha.format(formatter),
                    cantidadVentas,
                    montoTotal
            ));
        }

        return ventasPorDia;
    }
}