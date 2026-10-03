import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/auth_repository.dart';
import '../../data/catalogo_repository.dart';
import '../../data/dashboard_repository.dart';
import '../../data/ventas_repository.dart';
import '../../models/dashboard_resumen.dart';
import '../../models/producto_catalogo.dart';
import '../../models/sesion.dart';
import '../../models/venta.dart';
import '../../models/ventas_semanal.dart';
import '../../theme/app_colors.dart';
import '../acerca/acerca_screen.dart';
import '../ajustes/ajustes_screen.dart';
import '../../widgets/dialogo_actualizacion.dart';
import '../catalogo/catalogo_screen.dart';
import '../login/login_navegacion.dart';
import '../ventas/estado_entrega_ui.dart';
import '../ventas/nueva_venta_screen.dart';
import '../ventas/venta_detalle_screen.dart';
import '../ventas/ventas_screen.dart';

enum _EstadoResumen { cargando, conDatos, vacio, error }

const _diasSemana = <String>['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
const _mesesCortos = <String>['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/// Pantalla principal: resumen del día y accesos a los módulos.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.sesion});

  final Sesion sesion;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _authRepository = AuthRepository();
  final _dashboardRepository = DashboardRepository();
  final _ventasRepository = VentasRepository();
  final _catalogoRepository = CatalogoRepository();
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);
  final _formatoFecha = DateFormat('dd/MM/yyyy HH:mm');
  bool get _esAdmin => widget.sesion.usuario.role == 'ADMIN';

  _EstadoResumen _estado = _EstadoResumen.cargando;
  DashboardResumen? _resumen;
  VentasSemanal? _semana;

  /// Productos agotados o por debajo de su stock mínimo, los más urgentes
  /// primero. Alimentan la campana de notificaciones.
  List<ProductoCatalogo> _alertasStock = const <ProductoCatalogo>[];
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _cargarResumen();
    // Al abrir la app, avisa si hay una versión nueva (sin molestar si no hay internet).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) revisarActualizacionAlAbrir(context);
    });
  }

  Future<void> _cargarResumen() async {
    setState(() {
      _estado = _EstadoResumen.cargando;
      _errorMensaje = null;
    });

    try {
      final resumen = await _dashboardRepository.obtenerResumenDelDia(widget.sesion.token);
      // La semana es un extra de la pantalla: si falla no tumba el resumen
      // del día, la tarjeta simplemente queda sin cifra.
      VentasSemanal? semana;
      try {
        semana = await _dashboardRepository.obtenerVentasSemanal(widget.sesion.token);
      } catch (_) {
        semana = null;
      }
      // Igual que la semana: si falla, la campana queda sin avisos en vez de
      // tumbar el resumen del día.
      var alertas = const <ProductoCatalogo>[];
      try {
        alertas = await _catalogoRepository.obtenerBajoMinimo(widget.sesion.token);
        alertas = <ProductoCatalogo>[...alertas]..sort((a, b) => a.cantidadDisponible.compareTo(b.cantidadDisponible));
      } catch (_) {
        alertas = const <ProductoCatalogo>[];
      }
      if (!mounted) return;
      setState(() {
        _resumen = resumen;
        _semana = semana;
        _alertasStock = alertas;
        _estado = resumen.sinMovimientoHoy && (semana?.totalVentas ?? 0) == 0
            ? _EstadoResumen.vacio
            : _EstadoResumen.conDatos;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = error.mensaje;
        _estado = _EstadoResumen.error;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = 'No se pudo cargar el resumen del día.';
        _estado = _EstadoResumen.error;
      });
    }
  }

  Future<void> _cerrarSesion() async {
    await _authRepository.cerrarSesion();
    if (!mounted) return;
    irAlLogin(Navigator.of(context));
  }

  void _abrirCatalogo() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => CatalogoScreen(token: widget.sesion.token)),
    );
  }

  void _abrirVentas() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => VentasScreen(token: widget.sesion.token, esAdmin: _esAdmin)),
    );
  }

  /// Al tocar una tarjeta cuando el resumen no pudo cargar no se abre nada:
  /// aparece un mensaje corto abajo con el motivo y la opción de reintentar.
  void _mostrarErrorResumen() {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          // Con un botón de acción Flutter lo deja fijo hasta que se cierre a
          // mano; acá se quiere que se vaya solo a los 3 segundos.
          persist: false,
          duration: const Duration(seconds: 3),
          backgroundColor: AppColors.textoPrincipal,
          margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          content: Row(
            children: <Widget>[
              const Icon(Icons.cloud_off_rounded, color: Colors.white, size: 20),
              const SizedBox(width: 10),
              Expanded(
                child: Text(_errorMensaje ?? 'No se pudo cargar el resumen.'),
              ),
            ],
          ),
          action: SnackBarAction(
            label: 'Reintentar',
            textColor: const Color(0xFF8FD1AC),
            onPressed: _cargarResumen,
          ),
        ),
      );
  }

  /// Nueva venta como formulario flotante sobre el inicio, no como otra
  /// pantalla. Se cierra solo con la X (o al registrar la venta): sin deslizar
  /// ni tocar afuera, para no perder lo que ya se escribió por accidente.
  Future<void> _abrirNuevaVenta() async {
    final registrada = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (_) => FractionallySizedBox(
        heightFactor: 0.94,
        child: ClipRRect(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          child: NuevaVentaScreen(token: widget.sesion.token, esAdmin: _esAdmin),
        ),
      ),
    );
    if (registrada == true && mounted) _cargarResumen();
  }

  /// Tarjeta "Ventas hoy": arriba el total facturado del día y debajo las
  /// ventas registradas hoy.
  void _mostrarVentasHoy() {
    final hoy = DateTime.now();
    final ventasFuture = _ventasRepository.obtenerVentas(widget.sesion.token);

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (modalCtx) => Stack(
        children: <Widget>[
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => Navigator.of(modalCtx).pop(),
            ),
          ),
          DraggableScrollableSheet(
            initialChildSize: 0.75,
            minChildSize: 0.45,
            maxChildSize: 0.95,
            builder: (_, scrollController) => GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {},
              child: Container(
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
                ),
                child: Column(
                  children: <Widget>[
                    const SizedBox(height: 12),
                    Center(
                      child: Container(
                        width: 40,
                        height: 4,
                        decoration: BoxDecoration(
                          color: Colors.grey.shade300,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 14, 20, 12),
                      child: Row(
                        children: <Widget>[
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: const Color(0xFFE8F4EC),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(Icons.shopping_bag_outlined, color: AppColors.verdeOscuro, size: 22),
                          ),
                          const SizedBox(width: 12),
                          const Expanded(
                            child: Text(
                              'Ventas de Hoy',
                              style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Divider(height: 1),
                    Expanded(child: _contenidoVentasHoy(modalCtx, scrollController, hoy, ventasFuture)),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Una sola lista que se desplaza junta: la tarjeta de total y, debajo, las
  /// ventas COMPLETADA de hoy (las mismas que cuenta la tarjeta del inicio).
  Widget _contenidoVentasHoy(
    BuildContext modalCtx,
    ScrollController scrollController,
    DateTime hoy,
    Future<List<Venta>> ventasFuture,
  ) {
    return FutureBuilder<List<Venta>>(
      future: ventasFuture,
      builder: (context, snapshot) {
        final Widget ventas;
        if (snapshot.connectionState == ConnectionState.waiting) {
          ventas = const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro)),
          );
        } else if (snapshot.hasError) {
          ventas = Padding(
            padding: const EdgeInsets.all(20),
            child: Text(
              'Error al cargar ventas: ${snapshot.error}',
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.error),
            ),
          );
        } else {
          final ventasHoy = (snapshot.data ?? <Venta>[]).where((v) {
            if (v.fechaVenta == null || v.estado != EstadoVenta.completada) return false;
            final f = v.fechaVenta!;
            return f.year == hoy.year && f.month == hoy.month && f.day == hoy.day;
          }).toList();

          ventas = ventasHoy.isEmpty
              ? const Padding(
                  padding: EdgeInsets.all(30),
                  child: Column(
                    children: <Widget>[
                      Icon(Icons.inbox_outlined, size: 48, color: AppColors.textoSecundario),
                      SizedBox(height: 12),
                      Text(
                        'No hay ventas registradas hoy.',
                        style: TextStyle(color: AppColors.textoSecundario, fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                )
              : Column(
                  children: <Widget>[
                    for (var i = 0; i < ventasHoy.length; i++) ...<Widget>[
                      if (i > 0) const SizedBox(height: 10),
                      _tarjetaVentaHoy(modalCtx, ventasHoy[i]),
                    ],
                  ],
                );
        }

        return ListView(
          controller: scrollController,
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: <Widget>[
            _tarjetaTotalHoy(),
            const SizedBox(height: 18),
            const Text(
              'Detalle',
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.textoPrincipal),
            ),
            const SizedBox(height: 10),
            ventas,
          ],
        );
      },
    );
  }

  /// Tarjeta verde con el total facturado hoy y la cantidad de ventas.
  Widget _tarjetaTotalHoy() {
    final datos = _resumen;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: <Color>[AppColors.verdeOscuro, Color(0xFF2C5E43)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(18),
        boxShadow: <BoxShadow>[
          BoxShadow(color: AppColors.verdeOscuro.withValues(alpha: 0.25), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'TOTAL FACTURADO HOY',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 11, fontWeight: FontWeight.w700, letterSpacing: 0.8),
          ),
          const SizedBox(height: 6),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.end,
            children: <Widget>[
              Text(
                _formatoMoneda.format(datos?.montoVentasHoy ?? 0.0),
                style: const TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w900),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(8)),
                child: Text(
                  '${datos?.totalVentasHoy ?? 0} ventas',
                  style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Una venta de hoy: cliente, hora, estado del pago y de la entrega, y monto.
  /// Al tocarla se abre su detalle.
  Widget _tarjetaVentaHoy(BuildContext modalCtx, Venta v) {
    return InkWell(
      onTap: () {
        Navigator.of(modalCtx).pop();
        Navigator.of(context)
            .push(
              MaterialPageRoute<void>(
                builder: (_) => VentaDetalleScreen(idVenta: v.id, token: widget.sesion.token, esAdmin: _esAdmin),
              ),
            )
            .then((_) {
              if (mounted) _cargarResumen();
            });
      },
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
        ),
        child: Row(
          children: <Widget>[
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(v.nombreCliente, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 2),
                  Text(
                    v.fechaVenta != null ? _formatoFecha.format(v.fechaVenta!) : 'Hoy',
                    style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario),
                  ),
                  const SizedBox(height: 6),
                  Wrap(
                    spacing: 6,
                    children: <Widget>[
                      if (v.tieneSaldoPendiente)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(color: const Color(0xFFFEF3C7), borderRadius: BorderRadius.circular(6)),
                          child: Text(
                            'Saldo: ${_formatoMoneda.format(v.saldoPendiente)}',
                            style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFFD97706)),
                          ),
                        )
                      else
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(color: const Color(0xFFDCFCE7), borderRadius: BorderRadius.circular(6)),
                          child: const Text(
                            'Pagada',
                            style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFF15803D)),
                          ),
                        ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: colorEstadoEntrega(v.estadoEntrega).withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          v.estadoEntrega.etiqueta,
                          style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: colorEstadoEntrega(v.estadoEntrega)),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: <Widget>[
                Text(
                  _formatoMoneda.format(v.montoTotal),
                  style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 15, color: AppColors.verdeOscuro),
                ),
                const SizedBox(height: 4),
                const Icon(Icons.chevron_right_rounded, size: 18, color: AppColors.textoSecundario),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// Tarjeta "Ventas semanales": acumulado de lunes a domingo, con navegación
  /// a semanas anteriores.
  void _mostrarVentasSemanales() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (modalCtx) => Stack(
        children: <Widget>[
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => Navigator.of(modalCtx).pop(),
            ),
          ),
          DraggableScrollableSheet(
            initialChildSize: 0.75,
            minChildSize: 0.45,
            maxChildSize: 0.95,
            builder: (_, scrollController) => GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {},
              child: Container(
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
                ),
                child: _HojaVentasSemanales(
                  repositorio: _dashboardRepository,
                  token: widget.sesion.token,
                  formatoMoneda: _formatoMoneda,
                  scrollController: scrollController,
                  semanaInicial: _semana,
                  ventasRepositorio: _ventasRepository,
                  onAbrirVenta: (venta) {
                    Navigator.of(context)
                        .push(
                          MaterialPageRoute<void>(
                            builder: (_) => VentaDetalleScreen(
                              idVenta: venta.id,
                              token: widget.sesion.token,
                              esAdmin: _esAdmin,
                            ),
                          ),
                        )
                        .then((_) {
                          if (mounted) _cargarResumen();
                        });
                  },
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _mostrarVentasPorCobrar() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (modalCtx) => Stack(
        children: <Widget>[
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => Navigator.of(modalCtx).pop(),
            ),
          ),
          DraggableScrollableSheet(
            initialChildSize: 0.75,
            minChildSize: 0.45,
            maxChildSize: 0.95,
            builder: (_, scrollController) => GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {},
              child: Container(
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
                ),
          child: Column(
            children: <Widget>[
              const SizedBox(height: 12),
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: Colors.grey.shade300, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 14, 20, 10),
                child: Row(
                  children: <Widget>[
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(color: const Color(0xFFFFFBEB), borderRadius: BorderRadius.circular(12)),
                      child: const Icon(Icons.hourglass_bottom_rounded, color: Color(0xFFD97706), size: 22),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          const Text('Ventas por Cobrar', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal)),
                          Text('Saldo total: ${_formatoMoneda.format(_resumen?.montoCuotasPendientes ?? 0)} (${_resumen?.cuotasPendientes ?? 0} cuotas)', style: const TextStyle(fontSize: 12, color: AppColors.textoSecundario)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: FutureBuilder<List<Venta>>(
                  future: _ventasRepository.obtenerVentas(widget.sesion.token),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro));
                    }
                    if (snapshot.hasError) {
                      return Center(
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Text('Error al cargar ventas: ${snapshot.error}', textAlign: TextAlign.center, style: const TextStyle(color: AppColors.error)),
                        ),
                      );
                    }
                    final lista = snapshot.data ?? <Venta>[];
                    final porCobrar = lista.where((v) => v.tieneSaldoPendiente).toList();

                    if (porCobrar.isEmpty) {
                      return const Center(
                        child: Padding(
                          padding: EdgeInsets.all(30),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Icon(Icons.check_circle_outline, size: 48, color: Color(0xFF16A34A)),
                              SizedBox(height: 12),
                              Text('No hay ventas con saldo pendiente de pago.', style: TextStyle(color: AppColors.textoSecundario, fontWeight: FontWeight.w600)),
                            ],
                          ),
                        ),
                      );
                    }

                    return ListView.separated(
                      controller: scrollController,
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      itemCount: porCobrar.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final v = porCobrar[index];
                        return InkWell(
                          onTap: () {
                            Navigator.of(modalCtx).pop();
                            Navigator.of(this.context).push(
                              MaterialPageRoute<void>(builder: (_) => VentaDetalleScreen(idVenta: v.id, token: widget.sesion.token, esAdmin: _esAdmin)),
                            ).then((_) {
                              if (mounted) _cargarResumen();
                            });
                          },
                          borderRadius: BorderRadius.circular(14),
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: const Color(0xFFD97706).withValues(alpha: 0.25)),
                            ),
                            child: Row(
                              children: <Widget>[
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: <Widget>[
                                      Text(v.nombreCliente, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                                      if (v.telefonoCliente != null && v.telefonoCliente!.isNotEmpty) ...<Widget>[
                                        const SizedBox(height: 2),
                                        Text('Cel: ${v.telefonoCliente}', style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
                                      ],
                                      const SizedBox(height: 6),
                                      Text('Total venta: ${_formatoMoneda.format(v.montoTotal)}', style: const TextStyle(fontSize: 11.5, color: AppColors.textoSecundario)),
                                    ],
                                  ),
                                ),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: <Widget>[
                                    const Text('Debe:', style: TextStyle(fontSize: 10.5, color: Color(0xFFD97706), fontWeight: FontWeight.bold)),
                                    Text(_formatoMoneda.format(v.saldoPendiente), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 15, color: Color(0xFFD97706))),
                                    const SizedBox(height: 4),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                      decoration: BoxDecoration(color: const Color(0xFFFEF3C7), borderRadius: BorderRadius.circular(6)),
                                      child: const Text('Cobrar >', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFFD97706))),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  ],
),
    );
  }

  void _mostrarVentasPorEntregar() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (modalCtx) => Stack(
        children: <Widget>[
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => Navigator.of(modalCtx).pop(),
            ),
          ),
          DraggableScrollableSheet(
            initialChildSize: 0.75,
            minChildSize: 0.45,
            maxChildSize: 0.95,
            builder: (_, scrollController) => GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {},
              child: Container(
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
                ),
          child: Column(
            children: <Widget>[
              const SizedBox(height: 12),
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: Colors.grey.shade300, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 14, 20, 10),
                child: Row(
                  children: <Widget>[
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(color: const Color(0xFFF3E8FF), borderRadius: BorderRadius.circular(12)),
                      child: const Icon(Icons.local_shipping_outlined, color: Color(0xFF7C3AED), size: 22),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          const Text('Ventas por Entregar', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal)),
                          Text('${_resumen?.ventasPorEntregar ?? 0} pedidos por entregar', style: const TextStyle(fontSize: 12, color: AppColors.textoSecundario)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: FutureBuilder<List<Venta>>(
                  future: _ventasRepository.obtenerVentas(widget.sesion.token),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro));
                    }
                    if (snapshot.hasError) {
                      return Center(
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Text('Error al cargar entregas: ${snapshot.error}', textAlign: TextAlign.center, style: const TextStyle(color: AppColors.error)),
                        ),
                      );
                    }
                    final lista = snapshot.data ?? <Venta>[];
                    final porEntregar = lista.where((v) => v.porEntregar).toList();

                    if (porEntregar.isEmpty) {
                      return const Center(
                        child: Padding(
                          padding: EdgeInsets.all(30),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Icon(Icons.check_circle_outline, size: 48, color: Color(0xFF16A34A)),
                              SizedBox(height: 12),
                              Text('No hay pedidos pendientes de entrega.', style: TextStyle(color: AppColors.textoSecundario, fontWeight: FontWeight.w600)),
                            ],
                          ),
                        ),
                      );
                    }

                    return ListView.separated(
                      controller: scrollController,
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      itemCount: porEntregar.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final v = porEntregar[index];
                        final esDomicilio = v.modalidadEntrega == ModalidadEntrega.domicilio;
                        return InkWell(
                          onTap: () {
                            Navigator.of(modalCtx).pop();
                            Navigator.of(this.context).push(
                              MaterialPageRoute<void>(builder: (_) => VentaDetalleScreen(idVenta: v.id, token: widget.sesion.token, esAdmin: _esAdmin)),
                            ).then((_) {
                              if (mounted) _cargarResumen();
                            });
                          },
                          borderRadius: BorderRadius.circular(14),
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: const Color(0xFF7C3AED).withValues(alpha: 0.25)),
                            ),
                            child: Row(
                              children: <Widget>[
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: <Widget>[
                                      Row(
                                        children: <Widget>[
                                          Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                            decoration: BoxDecoration(color: const Color(0xFFF3E8FF), borderRadius: BorderRadius.circular(6)),
                                            child: Text(
                                              esDomicilio ? 'A domicilio' : 'Transportadora',
                                              style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFF7C3AED)),
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          Expanded(
                                            child: Text(v.nombreCliente, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13.5), overflow: TextOverflow.ellipsis),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 6),
                                      if (v.direccionDestino != null && v.direccionDestino!.isNotEmpty)
                                        Text('Dir: ${v.direccionDestino}', style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario), maxLines: 1, overflow: TextOverflow.ellipsis),
                                      if (v.ciudad != null && v.ciudad!.isNotEmpty)
                                        Text('Ciudad: ${v.ciudad}', style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
                                      if (v.transportadora != null && v.transportadora!.isNotEmpty)
                                        Text('Empresa: ${v.transportadora}', style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
                                    ],
                                  ),
                                ),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: <Widget>[
                                    Text(_formatoMoneda.format(v.montoTotal), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14)),
                                    const SizedBox(height: 4),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                      decoration: BoxDecoration(color: const Color(0xFFE0E7FF), borderRadius: BorderRadius.circular(6)),
                                      child: const Text('Entregar >', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFF4338CA))),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  ],
),
    );
  }

  @override
  Widget build(BuildContext context) {
    final usuario = widget.sesion.usuario;

    return Scaffold(
      appBar: AppBar(
        // Menú de 3 barras con el mismo diseño que el de la app anterior (un
        // botón cuadrado blanco que abre un menú debajo). Por ahora las
        // opciones no llevan a ninguna pantalla.
        leading: PopupMenuButton<String>(
          tooltip: 'Menú',
          offset: const Offset(0, 48),
          elevation: 6,
          color: Colors.white,
          surfaceTintColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: AppColors.verdeOscuro.withValues(alpha: 0.1)),
          ),
          onSelected: (opcion) {
            if (opcion == 'ajustes') {
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (_) => AjustesScreen(usuario: widget.sesion.usuario, onCerrarSesion: _cerrarSesion),
                ),
              );
            } else if (opcion == 'acerca') {
              Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const AcercaScreen()));
            }
          },
          itemBuilder: (_) => const <PopupMenuEntry<String>>[
            PopupMenuItem<String>(
              value: 'ajustes',
              child: Row(
                children: <Widget>[
                  Icon(Icons.settings_outlined, color: AppColors.verdeOscuro, size: 20),
                  SizedBox(width: 12),
                  Text(
                    'Ajustes',
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5, color: AppColors.textoPrincipal),
                  ),
                ],
              ),
            ),
            PopupMenuDivider(height: 1),
            PopupMenuItem<String>(
              value: 'acerca',
              child: Row(
                children: <Widget>[
                  Icon(Icons.info_outline_rounded, color: AppColors.verdeOscuro, size: 20),
                  SizedBox(width: 12),
                  Text(
                    'Acerca de la app',
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5, color: AppColors.textoPrincipal),
                  ),
                ],
              ),
            ),
          ],
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
                boxShadow: <BoxShadow>[
                  BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 2)),
                ],
              ),
              child: const Center(
                child: Icon(Icons.menu_rounded, color: AppColors.verdeOscuro, size: 22),
              ),
            ),
          ),
        ),
        title: const Text('Mueblería Edén'),
        actions: <Widget>[
          // Notificaciones: se abre justo debajo de la campana. Por ahora avisa
          // del stock bajo.
          PopupMenuButton<void>(
            tooltip: 'Notificaciones',
            icon: _CampanaConAviso(
              agotados: _alertasStock.where((p) => p.agotado).length,
              hayBajos: _alertasStock.isNotEmpty,
            ),
            position: PopupMenuPosition.under,
            color: Colors.white,
            surfaceTintColor: Colors.transparent,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            itemBuilder: (_) => <PopupMenuEntry<void>>[
              PopupMenuItem<void>(
                enabled: false,
                padding: EdgeInsets.zero,
                child: _PanelNotificaciones(alertas: _alertasStock),
              ),
            ],
          ),
          IconButton(
            tooltip: 'Cerrar sesión',
            icon: const Icon(Icons.logout_rounded),
            onPressed: _cerrarSesion,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.verdeAgua,
        foregroundColor: Colors.white,
        onPressed: _abrirNuevaVenta,
        icon: const Icon(Icons.add),
        label: const Text('Nueva venta'),
      ),
      // Tocar en cualquier parte fuera del aviso de error lo cierra.
      body: Listener(
        behavior: HitTestBehavior.translucent,
        onPointerDown: (_) => ScaffoldMessenger.of(context).hideCurrentSnackBar(),
        child: RefreshIndicator(
          color: AppColors.verdeOscuro,
          onRefresh: _cargarResumen,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(18, 18, 18, 100),
            children: <Widget>[
              Text(
                '¡Hola, ${usuario.nombre}!',
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textoPrincipal,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                usuario.role == 'ADMIN' ? 'Administrador' : 'Empleado',
                style: const TextStyle(color: AppColors.textoSecundario),
              ),
              const SizedBox(height: 20),
              const _TituloSeccion(texto: 'Resumen de hoy'),
              const SizedBox(height: 12),
              _CuerpoResumen(
                estado: _estado,
                resumen: _resumen,
                semana: _semana,
                errorMensaje: _errorMensaje,
                formatoMoneda: _formatoMoneda,
                onReintentar: _cargarResumen,
                onTapVentas: _mostrarVentasHoy,
                onTapSemanal: _mostrarVentasSemanales,
                onTapPorCobrar: _mostrarVentasPorCobrar,
                onTapPorEntregar: _mostrarVentasPorEntregar,
                onTapError: _mostrarErrorResumen,
              ),
              const SizedBox(height: 24),
              const _TituloSeccion(texto: 'Accesos rápidos'),
              const SizedBox(height: 12),
              Row(
                children: <Widget>[
                  Expanded(
                    child: _BotonModulo(
                      icono: Icons.local_shipping_outlined,
                      titulo: 'Ventas y entregas',
                      onTap: _abrirVentas,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _BotonModulo(
                      icono: Icons.bed_outlined,
                      titulo: 'Catálogo',
                      onTap: _abrirCatalogo,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TituloSeccion extends StatelessWidget {
  const _TituloSeccion({required this.texto});

  final String texto;

  @override
  Widget build(BuildContext context) {
    return Text(
      texto,
      style: const TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.bold,
        color: AppColors.textoPrincipal,
      ),
    );
  }
}

/// Los cuatro estados de la vista: cargando, con datos, vacío y error.
class _CuerpoResumen extends StatelessWidget {
  const _CuerpoResumen({
    required this.estado,
    required this.resumen,
    required this.semana,
    required this.errorMensaje,
    required this.formatoMoneda,
    required this.onReintentar,
    this.onTapVentas,
    this.onTapSemanal,
    this.onTapPorCobrar,
    this.onTapPorEntregar,
    this.onTapError,
  });

  final _EstadoResumen estado;
  final DashboardResumen? resumen;
  final VentasSemanal? semana;
  final String? errorMensaje;
  final NumberFormat formatoMoneda;
  final VoidCallback onReintentar;
  final VoidCallback? onTapVentas;
  final VoidCallback? onTapSemanal;
  final VoidCallback? onTapPorCobrar;
  final VoidCallback? onTapPorEntregar;

  /// Al tocar una tarjeta mientras hay un error.
  final VoidCallback? onTapError;

  @override
  Widget build(BuildContext context) {
    switch (estado) {
      case _EstadoResumen.cargando:
        return const Card(
          child: Padding(
            padding: EdgeInsets.symmetric(vertical: 32),
            child: Center(
              child: CircularProgressIndicator(color: AppColors.verdeOscuro),
            ),
          ),
        );

      case _EstadoResumen.error:
        // Las cuatro tarjetas se quedan en su lugar, con "—" en vez de un cero
        // que parecería un dato real, y un solo aviso arriba con el reintento.
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            _avisoError(),
            const SizedBox(height: 10),
            _rejilla(null),
          ],
        );

      case _EstadoResumen.vacio:
        return const Card(
          child: Padding(
            padding: EdgeInsets.all(20),
            child: Column(
              children: <Widget>[
                Icon(Icons.inbox_outlined, color: AppColors.textoSecundario, size: 40),
                SizedBox(height: 10),
                Text(
                  'Todavía no hay ventas registradas hoy.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppColors.textoSecundario),
                ),
              ],
            ),
          ),
        );

      case _EstadoResumen.conDatos:
        return _rejilla(resumen!);
    }
  }

  /// Aviso delgado con el motivo del error y el botón de reintentar.
  Widget _avisoError() {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 6, 6, 6),
      decoration: BoxDecoration(
        color: AppColors.errorFondo,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: <Widget>[
          const Icon(Icons.cloud_off_rounded, color: AppColors.error, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              errorMensaje ?? 'No se pudo cargar el resumen.',
              style: const TextStyle(fontSize: 12.5, color: AppColors.textoPrincipal),
            ),
          ),
          TextButton(onPressed: onReintentar, child: const Text('Reintentar')),
        ],
      ),
    );
  }

  /// Las cuatro tarjetas. Con [datos] nulo (error) muestran "—" y, al tocarlas,
  /// abren el detalle del error en vez de su hoja.
  Widget _rejilla(DashboardResumen? datos) {
    final sinDatos = datos == null;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: <Widget>[
            Row(
              children: <Widget>[
                Expanded(
                  child: _MetricaResumen(
                    icono: Icons.shopping_bag_outlined,
                    valor: sinDatos ? '—' : '${datos.totalVentasHoy}',
                    etiqueta: 'Ventas hoy',
                    onTap: sinDatos ? onTapError : onTapVentas,
                    mostrarVerDetalle: !sinDatos,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _MetricaResumen(
                    icono: Icons.date_range_rounded,
                    valor: semana != null ? formatoMoneda.format(semana!.montoTotal) : '—',
                    etiqueta: 'Ventas semanales',
                    onTap: sinDatos ? onTapError : onTapSemanal,
                    mostrarVerDetalle: !sinDatos,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: <Widget>[
                Expanded(
                  child: _MetricaResumen(
                    icono: Icons.hourglass_bottom_rounded,
                    valor: sinDatos ? '—' : formatoMoneda.format(datos.montoCuotasPendientes),
                    etiqueta: sinDatos ? 'Por cobrar' : 'Por cobrar (${datos.cuotasPendientes})',
                    onTap: sinDatos ? onTapError : onTapPorCobrar,
                    mostrarVerDetalle: !sinDatos,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _MetricaResumen(
                    icono: Icons.local_shipping_outlined,
                    valor: sinDatos ? '—' : '${datos.ventasPorEntregar}',
                    etiqueta: 'Por entregar',
                    onTap: sinDatos ? onTapError : onTapPorEntregar,
                    mostrarVerDetalle: !sinDatos,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _MetricaResumen extends StatelessWidget {
  const _MetricaResumen({
    required this.icono,
    required this.valor,
    required this.etiqueta,
    this.onTap,
    this.mostrarVerDetalle = true,
  });

  final IconData icono;
  final String valor;
  final String etiqueta;
  final VoidCallback? onTap;

  /// Falso mientras hay un error: la tarjeta todavía no tiene detalle que ver.
  final bool mostrarVerDetalle;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        splashColor: AppColors.verdeOscuro.withValues(alpha: 0.08),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 10),
          decoration: BoxDecoration(
            color: AppColors.fondoResumen,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.bordeResumen),
          ),
          child: Column(
            children: <Widget>[
              Icon(icono, color: AppColors.iconoResumen, size: 20),
              const SizedBox(height: 6),
              Text(
                valor,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w800,
                  color: AppColors.valorResumen,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                etiqueta,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11, color: AppColors.etiquetaResumen),
              ),
              const SizedBox(height: 4),
              if (onTap != null && mostrarVerDetalle)
                Text(
                  'Ver detalle ›',
                  style: TextStyle(fontSize: 9.5, fontWeight: FontWeight.w700, color: AppColors.iconoResumen.withValues(alpha: 0.6)),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Acceso a un módulo como botón suelto, cuadrado de esquinas redondeadas y de
/// un solo color.
class _BotonModulo extends StatelessWidget {
  const _BotonModulo({required this.icono, required this.titulo, required this.onTap});

  final IconData icono;
  final String titulo;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.fondoModulos,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: AppColors.bordeModulos),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Container(
          height: 56,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Row(
            children: <Widget>[
              Icon(icono, color: AppColors.iconoModulos, size: 20),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  titulo,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textoModulos,
                    height: 1.15,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Contenido de la hoja "Ventas semanales": resumen de la semana (lunes a
/// domingo), desglose por día y flechas para ir a semanas anteriores.
class _HojaVentasSemanales extends StatefulWidget {
  const _HojaVentasSemanales({
    required this.repositorio,
    required this.token,
    required this.formatoMoneda,
    required this.scrollController,
    required this.semanaInicial,
    required this.ventasRepositorio,
    required this.onAbrirVenta,
  });

  final DashboardRepository repositorio;
  final String token;
  final NumberFormat formatoMoneda;
  final ScrollController scrollController;
  final VentasRepository ventasRepositorio;

  /// Se llama al tocar una venta dentro de un día desplegado.
  final void Function(Venta venta) onAbrirVenta;

  /// La semana en curso ya cargada en la pantalla principal, para no volver a
  /// pedirla al abrir la hoja.
  final VentasSemanal? semanaInicial;

  @override
  State<_HojaVentasSemanales> createState() => _HojaVentasSemanalesState();
}

class _HojaVentasSemanalesState extends State<_HojaVentasSemanales> {
  final _formatoDiaMes = DateFormat('dd/MM');

  /// Semanas rápidas del desplegable: esta, la anterior y hace 2 y 3 semanas.
  /// Para cualquier otra se usa "Elegir otra fecha…".
  static const _totalSemanas = 4;

  /// Valores propios del desplegable que no son una semana rápida.
  static const _semanaPersonalizada = 4;
  static const _accionElegirFecha = 99;

  /// 0 = esta semana, 1 = semana anterior, 2 = hace 2 semanas, 3 = hace 3
  /// semanas, 4 = una semana elegida en el calendario.
  int _indice = 0;

  /// Lunes de la semana elegida en el calendario (solo con _indice == 4).
  DateTime? _lunesElegido;

  /// Día desplegado (solo uno a la vez) y la lista de ventas que se pide la
  /// primera vez que se despliega alguno.
  DateTime? _diaAbierto;
  Future<List<Venta>>? _ventasFuture;

  late Future<VentasSemanal> _futuro;

  @override
  void initState() {
    super.initState();
    _futuro = widget.semanaInicial != null
        ? Future<VentasSemanal>.value(widget.semanaInicial)
        : widget.repositorio.obtenerVentasSemanal(widget.token);
  }

  void _elegir(int indice) {
    setState(() {
      _indice = indice;
      _lunesElegido = null;
      _diaAbierto = null;
      _futuro = widget.repositorio.obtenerVentasSemanal(
        widget.token,
        fecha: _lunesDe(indice),
      );
    });
  }

  /// Semana lejana elegida en el calendario: queda como una opción más del
  /// desplegable, con sus fechas.
  void _elegirLunes(DateTime lunes) {
    setState(() {
      _indice = _semanaPersonalizada;
      _lunesElegido = lunes;
      _diaAbierto = null;
      _futuro = widget.repositorio.obtenerVentasSemanal(
        widget.token,
        fecha: lunes,
      );
    });
  }

  /// Ventana centrada para ir a cualquier semana: se toca un día y se pinta la
  /// fila completa (lunes a domingo) que lo contiene.
  Future<void> _abrirCalendario() async {
    final lunes = await showDialog<DateTime>(
      context: context,
      builder: (_) => _SelectorSemanaDialog(
        lunesInicial: _indice == _semanaPersonalizada && _lunesElegido != null
            ? _lunesElegido!
            : _lunesDe(_indice),
        hoy: DateTime.now(),
      ),
    );
    if (lunes == null || !mounted) return;

    final atras = _lunesDe(0).difference(lunes).inDays ~/ 7;
    if (atras >= 0 && atras < _totalSemanas) {
      _elegir(atras); // cae en una de las semanas rápidas: se usa esa
    } else {
      _elegirLunes(lunes);
    }
  }

  bool _mismoDia(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  void _alternarDia(DateTime fecha) {
    final abriendo = _diaAbierto == null || !_mismoDia(_diaAbierto!, fecha);
    setState(() {
      if (abriendo) {
        _diaAbierto = fecha;
        _ventasFuture ??= widget.ventasRepositorio.obtenerVentas(widget.token);
      } else {
        _diaAbierto = null;
      }
    });
  }

  /// Ventas completadas de [fecha], para mostrar dentro del día desplegado.
  Widget _ventasDelDia(DateTime fecha) {
    return FutureBuilder<List<Venta>>(
      future: _ventasFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Padding(
            padding: EdgeInsets.all(16),
            child: Center(
              child: SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  color: AppColors.verdeOscuro,
                  strokeWidth: 2,
                ),
              ),
            ),
          );
        }
        if (snapshot.hasError) {
          return const Padding(
            padding: EdgeInsets.all(14),
            child: Text(
              'No se pudieron cargar las ventas.',
              style: TextStyle(fontSize: 12, color: AppColors.error),
            ),
          );
        }
        final ventas = (snapshot.data ?? <Venta>[])
            .where(
              (v) =>
                  v.fechaVenta != null &&
                  v.estado == EstadoVenta.completada &&
                  _mismoDia(v.fechaVenta!, fecha),
            )
            .toList();
        final formatoHora = DateFormat('HH:mm');

        return Column(
          children: <Widget>[
            Divider(
              height: 1,
              color: AppColors.verdeOscuro.withValues(alpha: 0.12),
            ),
            for (final v in ventas)
              InkWell(
                onTap: () => widget.onAbrirVenta(v),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 10,
                  ),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              v.nombreCliente,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: AppColors.textoPrincipal,
                              ),
                            ),
                            Text(
                              v.tieneSaldoPendiente
                                  ? '${formatoHora.format(v.fechaVenta!)} · Saldo ${widget.formatoMoneda.format(v.saldoPendiente)}'
                                  : '${formatoHora.format(v.fechaVenta!)} · Pagada',
                              style: TextStyle(
                                fontSize: 11,
                                color: v.tieneSaldoPendiente
                                    ? const Color(0xFFD97706)
                                    : AppColors.textoSecundario,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Text(
                        widget.formatoMoneda.format(v.montoTotal),
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.verdeOscuro,
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(
                        Icons.chevron_right_rounded,
                        size: 18,
                        color: AppColors.textoSecundario,
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }

  /// Lunes de la semana que está [atras] semanas antes de la actual.
  DateTime _lunesDe(int atras) {
    final hoy = DateTime.now();
    return DateTime(
      hoy.year,
      hoy.month,
      hoy.day - (hoy.weekday - 1) - 7 * atras,
    );
  }

  String _rango(int atras) => _rangoDe(_lunesDe(atras));

  /// "21 a 27 sep", o "28 sep a 4 oct" si la semana cruza de mes.
  String _rangoDe(DateTime inicio) {
    final fin = DateTime(inicio.year, inicio.month, inicio.day + 6);
    if (inicio.month == fin.month) {
      return '${inicio.day} a ${fin.day} ${_mesesCortos[fin.month - 1]}';
    }
    return '${inicio.day} ${_mesesCortos[inicio.month - 1]} a ${fin.day} ${_mesesCortos[fin.month - 1]}';
  }

  /// Nombre de las cuatro semanas rápidas, con sus fechas.
  String _etiqueta(int atras) {
    switch (atras) {
      case 0:
        return 'Esta semana · ${_rango(atras)}';
      case 1:
        return 'Semana anterior · ${_rango(atras)}';
      default:
        return 'Hace $atras semanas · ${_rango(atras)}';
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<VentasSemanal>(
      future: _futuro,
      builder: (context, snapshot) {
        final semana = snapshot.data;
        final cargando = snapshot.connectionState == ConnectionState.waiting;

        // Las semanas rápidas y, si se eligió una en el calendario, esa también.
        final opciones = <MapEntry<int, String>>[
          for (var i = 0; i < _totalSemanas; i++)
            MapEntry<int, String>(i, _etiqueta(i)),
          if (_lunesElegido != null)
            MapEntry<int, String>(
              _semanaPersonalizada,
              _rangoDe(_lunesElegido!),
            ),
        ];

        return Column(
          children: <Widget>[
            const SizedBox(height: 12),
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 6),
              child: Row(
                children: <Widget>[
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFE8F4EC),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(
                      Icons.date_range_rounded,
                      color: AppColors.verdeOscuro,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          'Ventas Semanales',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textoPrincipal,
                          ),
                        ),
                        Text(
                          'De lunes a domingo · ventas completadas',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.textoSecundario,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
              child: Container(
                height: 46,
                padding: const EdgeInsets.symmetric(horizontal: 14),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: AppColors.verdeOscuro.withValues(alpha: 0.25),
                  ),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: _indice,
                    isExpanded: true,
                    dropdownColor: Colors.white,
                    borderRadius: BorderRadius.circular(14),
                    icon: const Icon(
                      Icons.keyboard_arrow_down_rounded,
                      color: AppColors.verdeOscuro,
                    ),
                    onChanged: cargando
                        ? null
                        : (v) {
                            if (v == null) return;
                            if (v == _accionElegirFecha) {
                              _abrirCalendario();
                            } else if (v != _semanaPersonalizada) {
                              _elegir(v);
                            }
                          },
                    selectedItemBuilder: (_) => <Widget>[
                      for (final o in opciones)
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Text(
                            o.value,
                            style: const TextStyle(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w700,
                              color: AppColors.textoPrincipal,
                            ),
                          ),
                        ),
                      const SizedBox.shrink(), // "Elegir otra fecha…" nunca queda como valor elegido
                    ],
                    items: <DropdownMenuItem<int>>[
                      for (final o in opciones)
                        DropdownMenuItem<int>(
                          value: o.key,
                          child: Text(
                            o.value,
                            style: TextStyle(
                              fontSize: 13.5,
                              fontWeight: o.key == _indice
                                  ? FontWeight.w800
                                  : FontWeight.w500,
                              color: AppColors.textoPrincipal,
                            ),
                          ),
                        ),
                      const DropdownMenuItem<int>(
                        value: _accionElegirFecha,
                        child: Row(
                          children: <Widget>[
                            Icon(
                              Icons.calendar_month_outlined,
                              size: 18,
                              color: AppColors.verdeOscuro,
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Elegir otra fecha…',
                              style: TextStyle(
                                fontSize: 13.5,
                                fontWeight: FontWeight.w600,
                                color: AppColors.verdeOscuro,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const Divider(height: 1),
            Expanded(child: _cuerpo(snapshot, semana, cargando)),
          ],
        );
      },
    );
  }

  Widget _cuerpo(
    AsyncSnapshot<VentasSemanal> snapshot,
    VentasSemanal? semana,
    bool cargando,
  ) {
    if (cargando) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.verdeOscuro),
      );
    }
    if (snapshot.hasError || semana == null) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(20),
          child: Text(
            'No se pudieron cargar las ventas de la semana.',
            textAlign: TextAlign.center,
            style: TextStyle(color: AppColors.error),
          ),
        ),
      );
    }

    final hoy = DateTime.now();

    return ListView(
      controller: widget.scrollController,
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 30),
      children: <Widget>[
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: <Color>[AppColors.verdeOscuro, Color(0xFF2C5E43)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(18),
            boxShadow: <BoxShadow>[
              BoxShadow(
                color: AppColors.verdeOscuro.withValues(alpha: 0.25),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                semana.esSemanaActual
                    ? 'ACUMULADO DE LA SEMANA'
                    : 'TOTAL DE LA SEMANA',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.8),
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.8,
                ),
              ),
              const SizedBox(height: 6),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: <Widget>[
                  Text(
                    widget.formatoMoneda.format(semana.montoTotal),
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 26,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '${semana.totalVentas} ventas',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        const Text(
          'Desglose por día',
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.bold,
            color: AppColors.textoPrincipal,
          ),
        ),
        const SizedBox(height: 8),
        for (final dia in semana.ventasPorDia) ...<Widget>[
          _FilaDiaSemana(
            nombre: _diasSemana[dia.fecha.weekday - 1],
            fecha: _formatoDiaMes.format(dia.fecha),
            esHoy:
                dia.fecha.year == hoy.year &&
                dia.fecha.month == hoy.month &&
                dia.fecha.day == hoy.day,
            cantidad: dia.cantidadVentas,
            monto: widget.formatoMoneda.format(dia.montoTotal),
            abierto: _diaAbierto != null && _mismoDia(_diaAbierto!, dia.fecha),
            onTap: dia.cantidadVentas > 0
                ? () => _alternarDia(dia.fecha)
                : null,
            contenido: dia.cantidadVentas > 0 ? _ventasDelDia(dia.fecha) : null,
          ),
          const SizedBox(height: 6),
        ],
      ],
    );
  }
}

class _FilaDiaSemana extends StatelessWidget {
  const _FilaDiaSemana({
    required this.nombre,
    required this.fecha,
    required this.esHoy,
    required this.cantidad,
    required this.monto,
    required this.abierto,
    this.onTap,
    this.contenido,
  });

  final String nombre;
  final String fecha;
  final bool esHoy;
  final int cantidad;
  final String monto;
  final bool abierto;

  /// null en los días sin ventas: no hay nada que desplegar.
  final VoidCallback? onTap;

  /// Lo que se muestra debajo de la fila cuando está desplegada.
  final Widget? contenido;

  @override
  Widget build(BuildContext context) {
    final sinVentas = cantidad == 0;
    return Material(
      color: esHoy ? const Color(0xFFE8F4EC) : const Color(0xFFF8FAF9),
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: AppColors.verdeOscuro.withValues(
            alpha: esHoy || abierto ? 0.3 : 0.1,
          ),
        ),
      ),
      child: Column(
        children: <Widget>[
          InkWell(
            onTap: onTap,
            // Mientras el día está desplegado, su encabezado lleva un toque de
            // verde; al cerrarlo (o abrir otro día) vuelve a su color normal.
            child: Ink(
              color: abierto
                  ? (esHoy ? const Color(0xFFDCEFE2) : const Color(0xFFE8F4EC))
                  : null,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 10,
                ),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            esHoy ? '$nombre · hoy' : nombre,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: sinVentas
                                  ? AppColors.textoSecundario
                                  : AppColors.textoPrincipal,
                            ),
                          ),
                          Text(
                            sinVentas
                                ? fecha
                                : '$fecha · $cantidad ${cantidad == 1 ? 'venta' : 'ventas'}',
                            style: const TextStyle(
                              fontSize: 11,
                              color: AppColors.textoSecundario,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      monto,
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.bold,
                        color: sinVentas
                            ? AppColors.textoSecundario
                            : AppColors.verdeOscuro,
                      ),
                    ),
                    if (onTap != null) ...<Widget>[
                      const SizedBox(width: 4),
                      Icon(
                        abierto
                            ? Icons.keyboard_arrow_up_rounded
                            : Icons.keyboard_arrow_down_rounded,
                        size: 20,
                        color: AppColors.textoSecundario,
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 200),
            curve: Curves.easeInOut,
            alignment: Alignment.topCenter,
            child: abierto && contenido != null
                ? contenido!
                : const SizedBox(width: double.infinity),
          ),
        ],
      ),
    );
  }
}

/// Ventana "Elegir semana", centrada en la pantalla: un calendario mensual
/// donde al tocar un día se pinta la semana completa (lunes a domingo) que lo
/// contiene. Arriba tiene desplegables de año, mes y día para saltar rápido.
/// Devuelve el lunes de la semana elegida, o null si se cierra con la X.
class _SelectorSemanaDialog extends StatefulWidget {
  const _SelectorSemanaDialog({required this.lunesInicial, required this.hoy});

  /// Semana que aparece elegida al abrir.
  final DateTime lunesInicial;
  final DateTime hoy;

  @override
  State<_SelectorSemanaDialog> createState() => _SelectorSemanaDialogState();
}

class _SelectorSemanaDialogState extends State<_SelectorSemanaDialog> {
  static const _mesesLargos = <String>[
    'enero',
    'febrero',
    'marzo',
    'abril',
    'mayo',
    'junio',
    'julio',
    'agosto',
    'septiembre',
    'octubre',
    'noviembre',
    'diciembre',
  ];
  static const _letrasDia = <String>['L', 'M', 'X', 'J', 'V', 'S', 'D'];

  /// Primer año que se puede consultar.
  static const _primerAnio = 2024;

  late final DateTime _hoy;

  /// Día 1 del mes que se está viendo.
  late DateTime _mes;

  /// Día tocado (o el inicial) y el lunes de su semana, que es la elegida.
  late DateTime _dia;
  late DateTime _lunes;

  @override
  void initState() {
    super.initState();
    _hoy = DateTime(widget.hoy.year, widget.hoy.month, widget.hoy.day);
    _lunes = widget.lunesInicial;
    final domingo = DateTime(_lunes.year, _lunes.month, _lunes.day + 6);
    // Al abrir se marca hoy si cae en la semana elegida; si no, el lunes.
    _dia = !_hoy.isBefore(_lunes) && !_hoy.isAfter(domingo) ? _hoy : _lunes;
    // Se abre en el mes donde cae el jueves de la semana elegida, que es el
    // mes al que la semana "pertenece" cuando cruza de un mes a otro. Nunca en
    // un mes futuro: la semana en curso puede terminar en el mes siguiente,
    // pero ese mes no se puede consultar todavía.
    final jueves = DateTime(_lunes.year, _lunes.month, _lunes.day + 3);
    final mesDelJueves = DateTime(jueves.year, jueves.month);
    final mesActual = DateTime(_hoy.year, _hoy.month);
    _mes = mesDelJueves.isAfter(mesActual) ? mesActual : mesDelJueves;
  }

  bool _mismoDia(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  DateTime _lunesDe(DateTime f) =>
      DateTime(f.year, f.month, f.day - (f.weekday - 1));

  bool get _puedeRetroceder => _mes.isAfter(DateTime(_primerAnio));

  bool get _puedeAvanzar => _mes.isBefore(DateTime(_hoy.year, _hoy.month));

  /// Último mes que se puede elegir en [anio]: diciembre, o el mes actual si
  /// es el año en curso.
  int _ultimoMesDe(int anio) => anio == _hoy.year ? _hoy.month : 12;

  /// Último día que se puede elegir en el mes que se está viendo: el fin de
  /// mes, o hoy si es el mes en curso.
  int get _ultimoDiaDelMes {
    final finDeMes = DateTime(_mes.year, _mes.month + 1, 0).day;
    return _mes.year == _hoy.year && _mes.month == _hoy.month
        ? _hoy.day
        : finDeMes;
  }

  void _cambiarMes(int delta) {
    setState(() => _mes = DateTime(_mes.year, _mes.month + delta));
  }

  void _elegirAnio(int anio) {
    final mes = _mes.month > _ultimoMesDe(anio)
        ? _ultimoMesDe(anio)
        : _mes.month;
    setState(() => _mes = DateTime(anio, mes));
  }

  void _elegirMes(int mes) {
    setState(() => _mes = DateTime(_mes.year, mes));
  }

  void _elegirDia(int dia) {
    setState(() {
      _dia = DateTime(_mes.year, _mes.month, dia);
      _lunes = _lunesDe(_dia);
    });
  }

  /// Lunes de cada fila del mes que se está viendo.
  List<DateTime> _lunesDelMes() {
    final ultimo = DateTime(_mes.year, _mes.month + 1, 0);
    final lunes = <DateTime>[];
    var l = _lunesDe(_mes);
    while (!l.isAfter(ultimo)) {
      lunes.add(l);
      l = DateTime(l.year, l.month, l.day + 7);
    }
    return lunes;
  }

  /// "Semana del 21 al 27 sep", o "Semana del 28 sep al 4 oct" si cruza de mes.
  String get _textoSemana {
    final fin = DateTime(_lunes.year, _lunes.month, _lunes.day + 6);
    if (_lunes.month == fin.month) {
      return 'Semana del ${_lunes.day} al ${fin.day} ${_mesesCortos[fin.month - 1]}';
    }
    return 'Semana del ${_lunes.day} ${_mesesCortos[_lunes.month - 1]} al ${fin.day} ${_mesesCortos[fin.month - 1]}';
  }

  String _capitalizada(String s) => '${s[0].toUpperCase()}${s.substring(1)}';

  Widget _desplegable<T>({
    required T? value,
    required String hint,
    required List<DropdownMenuItem<T>> items,
    required ValueChanged<T?> onChanged,
  }) {
    return Container(
      height: 44,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: AppColors.verdeOscuro.withValues(alpha: 0.25),
        ),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          hint: Text(
            hint,
            style: const TextStyle(
              fontSize: 13.5,
              color: AppColors.textoSecundario,
            ),
          ),
          isExpanded: true,
          dropdownColor: Colors.white,
          borderRadius: BorderRadius.circular(12),
          menuMaxHeight: 300,
          icon: const Icon(
            Icons.keyboard_arrow_down_rounded,
            size: 20,
            color: AppColors.verdeOscuro,
          ),
          style: const TextStyle(
            fontSize: 13.5,
            fontWeight: FontWeight.w700,
            color: AppColors.textoPrincipal,
          ),
          items: items,
          onChanged: onChanged,
        ),
      ),
    );
  }

  Widget _celda(DateTime f, DateTime lunesFila) {
    final futuro = f.isAfter(_hoy);
    final delMes = f.month == _mes.month;
    final esHoy = _mismoDia(f, _hoy);
    final elegida = _mismoDia(lunesFila, _lunes);
    final diaElegido = _mismoDia(f, _dia);

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: futuro
          ? null
          : () => setState(() {
              _dia = f;
              _lunes = lunesFila;
            }),
      child: Center(
        child: Container(
          width: 34,
          height: 34,
          alignment: Alignment.center,
          // El día tocado va en un círculo relleno (verde agua), distinto de la
          // banda de la semana y del aro de "hoy".
          decoration: diaElegido
              ? const BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.verdeAgua,
                )
              : esHoy
              ? BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.verdeOscuro, width: 1.5),
                )
              : null,
          child: Text(
            '${f.day}',
            style: TextStyle(
              fontSize: 14,
              fontWeight: elegida || esHoy || diaElegido
                  ? FontWeight.w800
                  : FontWeight.w500,
              color: diaElegido
                  ? Colors.white
                  : futuro
                  ? AppColors.textoSecundario.withValues(alpha: 0.4)
                  : delMes
                  ? AppColors.textoPrincipal
                  : AppColors.textoSecundario,
            ),
          ),
        ),
      ),
    );
  }

  Widget _fila(DateTime lunes) {
    final elegida = _mismoDia(lunes, _lunes);
    return Container(
      height: 44,
      margin: const EdgeInsets.symmetric(vertical: 2),
      decoration: BoxDecoration(
        color: elegida ? AppColors.verdeOscuro.withValues(alpha: 0.14) : null,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Row(
        children: <Widget>[
          for (var i = 0; i < 7; i++)
            Expanded(
              child: _celda(
                DateTime(lunes.year, lunes.month, lunes.day + i),
                lunes,
              ),
            ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final diaEnEsteMes = _dia.year == _mes.year && _dia.month == _mes.month;

    return Dialog(
      backgroundColor: Colors.white,
      surfaceTintColor: Colors.transparent,
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 12, 12, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Row(
                children: <Widget>[
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          'Elegir semana',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textoPrincipal,
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Toca un día y se elige toda su semana',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.textoSecundario,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Cerrar',
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      flex: 3,
                      child: _desplegable<int>(
                        value: _mes.year,
                        hint: 'Año',
                        items: <DropdownMenuItem<int>>[
                          for (var a = _hoy.year; a >= _primerAnio; a--)
                            DropdownMenuItem<int>(value: a, child: Text('$a')),
                        ],
                        onChanged: (a) {
                          if (a != null) _elegirAnio(a);
                        },
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      flex: 5,
                      child: _desplegable<int>(
                        value: _mes.month,
                        hint: 'Mes',
                        items: <DropdownMenuItem<int>>[
                          for (var m = 1; m <= _ultimoMesDe(_mes.year); m++)
                            DropdownMenuItem<int>(
                              value: m,
                              child: Text(_capitalizada(_mesesLargos[m - 1])),
                            ),
                        ],
                        onChanged: (m) {
                          if (m != null) _elegirMes(m);
                        },
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      flex: 3,
                      child: _desplegable<int>(
                        value: diaEnEsteMes ? _dia.day : null,
                        hint: 'Día',
                        items: <DropdownMenuItem<int>>[
                          for (var d = 1; d <= _ultimoDiaDelMes; d++)
                            DropdownMenuItem<int>(value: d, child: Text('$d')),
                        ],
                        onChanged: (d) {
                          if (d != null) _elegirDia(d);
                        },
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    const SizedBox(height: 4),
                    Row(
                      children: <Widget>[
                        IconButton(
                          tooltip: 'Mes anterior',
                          icon: const Icon(Icons.chevron_left_rounded),
                          onPressed: _puedeRetroceder
                              ? () => _cambiarMes(-1)
                              : null,
                        ),
                        Expanded(
                          child: Text(
                            '${_capitalizada(_mesesLargos[_mes.month - 1])} ${_mes.year}',
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: AppColors.textoPrincipal,
                            ),
                          ),
                        ),
                        IconButton(
                          tooltip: 'Mes siguiente',
                          icon: const Icon(Icons.chevron_right_rounded),
                          onPressed: _puedeAvanzar
                              ? () => _cambiarMes(1)
                              : null,
                        ),
                      ],
                    ),
                    Row(
                      children: <Widget>[
                        for (final letra in _letrasDia)
                          Expanded(
                            child: Center(
                              child: Text(
                                letra,
                                style: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textoSecundario,
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    for (final lunes in _lunesDelMes()) _fila(lunes),
                    const SizedBox(height: 12),
                    Text(
                      _textoSemana,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w700,
                        color: AppColors.verdeOscuro,
                      ),
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: () => Navigator.of(context).pop(_lunes),
                      child: const Text('Ver semana'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Campana de notificaciones con su aviso: un número rojo con los productos
/// agotados, o solo un punto naranja si hay stock bajo pero ninguno agotado.
/// Sin alertas, la campana va sola.
class _CampanaConAviso extends StatelessWidget {
  const _CampanaConAviso({required this.agotados, required this.hayBajos});

  final int agotados;
  final bool hayBajos;

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: <Widget>[
        const Icon(Icons.notifications_none_rounded),
        if (agotados > 0)
          Positioned(
            right: -6,
            top: -5,
            child: Container(
              constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
              padding: const EdgeInsets.symmetric(horizontal: 4),
              decoration: BoxDecoration(color: AppColors.error, borderRadius: BorderRadius.circular(8)),
              child: Center(
                child: Text(
                  '$agotados',
                  style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.w800, height: 1.2),
                ),
              ),
            ),
          )
        else if (hayBajos)
          Positioned(
            right: 1,
            top: 1,
            child: Container(
              width: 9,
              height: 9,
              decoration: const BoxDecoration(color: Color(0xFFF59E0B), shape: BoxShape.circle),
            ),
          ),
      ],
    );
  }
}

/// Lo que muestra la campana al abrirse: el stock bajo, con los agotados
/// primero, o un mensaje de que todo está en orden.
class _PanelNotificaciones extends StatelessWidget {
  const _PanelNotificaciones({required this.alertas});

  final List<ProductoCatalogo> alertas;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 290,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            const Text(
              'Notificaciones',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal),
            ),
            const SizedBox(height: 12),
            if (alertas.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Center(
                  child: Column(
                    children: <Widget>[
                      Icon(Icons.check_circle_outline_rounded, size: 40, color: AppColors.verdeSuave),
                      SizedBox(height: 8),
                      Text(
                        'Todo en orden',
                        style: TextStyle(fontSize: 13.5, fontWeight: FontWeight.w700, color: AppColors.textoPrincipal),
                      ),
                      SizedBox(height: 2),
                      Text(
                        'No hay productos con stock bajo.',
                        style: TextStyle(fontSize: 12, color: AppColors.textoSecundario),
                      ),
                    ],
                  ),
                ),
              )
            else ...<Widget>[
              const Text(
                'STOCK BAJO',
                style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 0.8, color: AppColors.textoSecundario),
              ),
              const SizedBox(height: 6),
              ConstrainedBox(
                constraints: const BoxConstraints(maxHeight: 300),
                child: SingleChildScrollView(
                  child: Column(
                    children: <Widget>[
                      for (final p in alertas)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 7),
                          child: Row(
                            children: <Widget>[
                              Container(
                                width: 9,
                                height: 9,
                                decoration: BoxDecoration(
                                  color: p.agotado ? AppColors.error : const Color(0xFFF59E0B),
                                  shape: BoxShape.circle,
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  p.nombre,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.textoPrincipal),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(
                                p.agotado ? 'Agotado' : 'Quedan ${p.cantidadDisponible}',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                  color: p.agotado ? AppColors.error : const Color(0xFFD97706),
                                ),
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
