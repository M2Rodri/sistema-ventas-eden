import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/auth_repository.dart';
import '../../data/dashboard_repository.dart';
import '../../data/ventas_repository.dart';
import '../../models/dashboard_resumen.dart';
import '../../models/sesion.dart';
import '../../models/venta.dart';
import '../../theme/app_colors.dart';
import '../alertas_stock/alertas_stock_screen.dart';
import '../catalogo/catalogo_screen.dart';
import '../login/login_screen.dart';
import '../ventas/estado_entrega_ui.dart';
import '../ventas/nueva_venta_screen.dart';
import '../ventas/venta_detalle_screen.dart';
import '../ventas/ventas_screen.dart';

enum _EstadoResumen { cargando, conDatos, vacio, error }

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
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);
  final _formatoFecha = DateFormat('dd/MM/yyyy HH:mm');
  bool get _esAdmin => widget.sesion.usuario.role == 'ADMIN';

  _EstadoResumen _estado = _EstadoResumen.cargando;
  DashboardResumen? _resumen;
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _cargarResumen();
  }

  Future<void> _cargarResumen() async {
    setState(() {
      _estado = _EstadoResumen.cargando;
      _errorMensaje = null;
    });

    try {
      final resumen = await _dashboardRepository.obtenerResumenDelDia(widget.sesion.token);
      if (!mounted) return;
      setState(() {
        _resumen = resumen;
        _estado = resumen.sinMovimientoHoy ? _EstadoResumen.vacio : _EstadoResumen.conDatos;
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
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute<void>(
        // Importante: acá adentro no hay que usar el "context" de
        // _HomeScreenState. pushAndRemoveUntil saca esta pantalla del árbol,
        // así que ese context queda inválido para cuando el usuario
        // finalmente inicia sesión de nuevo (es async, tarda). Se usa el
        // "routeContext" que entrega este mismo builder, que es el de la
        // pantalla de login recién creada y sigue vivo en ese momento.
        builder: (routeContext) => LoginScreen(
          onSesionIniciada: (sesion) {
            Navigator.of(routeContext).pushReplacement(
              MaterialPageRoute<void>(builder: (_) => HomeScreen(sesion: sesion)),
            );
          },
        ),
      ),
      (route) => false,
    );
  }

  void _abrirCatalogo() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => CatalogoScreen(token: widget.sesion.token)),
    );
  }

  void _abrirAlertasStock() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => AlertasStockScreen(token: widget.sesion.token)),
    );
  }

  void _abrirVentas() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => VentasScreen(token: widget.sesion.token, esAdmin: _esAdmin)),
    );
  }

  Future<void> _abrirNuevaVenta() async {
    final registrada = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(builder: (_) => NuevaVentaScreen(token: widget.sesion.token, esAdmin: _esAdmin)),
    );
    if (registrada == true && mounted) _cargarResumen();
  }

  void _mostrarVentasHoy() {
    final hoy = DateTime.now();
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
                            decoration: BoxDecoration(color: const Color(0xFFE8F4EC), borderRadius: BorderRadius.circular(12)),
                            child: const Icon(Icons.shopping_bag_outlined, color: AppColors.verdeOscuro, size: 22),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                const Text('Ventas de Hoy', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal)),
                                Text('${hoy.day}/${hoy.month}/${hoy.year} · Registradas en el sistema', style: const TextStyle(fontSize: 12, color: AppColors.textoSecundario)),
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
                          final ventasHoy = lista.where((v) {
                            if (v.fechaVenta == null) return false;
                            final f = v.fechaVenta!;
                            return f.year == hoy.year && f.month == hoy.month && f.day == hoy.day;
                          }).toList();

                          if (ventasHoy.isEmpty) {
                            return const Center(
                              child: Padding(
                                padding: EdgeInsets.all(30),
                                child: Column(
                                  mainAxisSize: MainAxisSize.min,
                                  children: <Widget>[
                                    Icon(Icons.inbox_outlined, size: 48, color: AppColors.textoSecundario),
                                    SizedBox(height: 12),
                                    Text('No hay ventas registradas hoy.', style: TextStyle(color: AppColors.textoSecundario, fontWeight: FontWeight.w600)),
                                  ],
                                ),
                              ),
                            );
                          }

                          return ListView.separated(
                            controller: scrollController,
                            padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                            itemCount: ventasHoy.length,
                            separatorBuilder: (_, _) => const SizedBox(height: 10),
                            itemBuilder: (context, index) {
                              final v = ventasHoy[index];
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
                                            Text(v.fechaVenta != null ? _formatoFecha.format(v.fechaVenta!) : 'Hoy', style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
                                            const SizedBox(height: 6),
                                            Wrap(
                                              spacing: 6,
                                              children: <Widget>[
                                                if (v.tieneSaldoPendiente)
                                                  Container(
                                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                                    decoration: BoxDecoration(color: const Color(0xFFFEF3C7), borderRadius: BorderRadius.circular(6)),
                                                    child: Text('Saldo: ${_formatoMoneda.format(v.saldoPendiente)}', style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFFD97706))),
                                                  )
                                                else
                                                  Container(
                                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                                    decoration: BoxDecoration(color: const Color(0xFFDCFCE7), borderRadius: BorderRadius.circular(6)),
                                                    child: const Text('Pagada', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFF15803D))),
                                                  ),
                                                Container(
                                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                                  decoration: BoxDecoration(color: colorEstadoEntrega(v.estadoEntrega).withValues(alpha: 0.12), borderRadius: BorderRadius.circular(6)),
                                                  child: Text(v.estadoEntrega.etiqueta, style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: colorEstadoEntrega(v.estadoEntrega))),
                                                ),
                                              ],
                                            ),
                                          ],
                                        ),
                                      ),
                                      Column(
                                        crossAxisAlignment: CrossAxisAlignment.end,
                                        children: <Widget>[
                                          Text(_formatoMoneda.format(v.montoTotal), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 15, color: AppColors.verdeOscuro)),
                                          const SizedBox(height: 4),
                                          const Icon(Icons.chevron_right_rounded, size: 18, color: AppColors.textoSecundario),
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

  void _mostrarBalanceHoy() {
    final hoy = DateTime.now();
    final datos = _resumen;
    final totalFacturado = datos?.montoVentasHoy ?? 0.0;
    final totalTransacciones = datos?.totalVentasHoy ?? 0;
    final ticketMedio = totalTransacciones > 0 ? totalFacturado / totalTransacciones : 0.0;

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
            initialChildSize: 0.70,
            minChildSize: 0.45,
            maxChildSize: 0.92,
            builder: (_, scrollController) => GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {},
              child: Container(
                decoration: const BoxDecoration(
                  color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: ListView(
            controller: scrollController,
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 30),
            children: <Widget>[
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: Colors.grey.shade300, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: <Widget>[
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(color: const Color(0xFFE8F4EC), borderRadius: BorderRadius.circular(12)),
                    child: const Icon(Icons.account_balance_wallet_rounded, color: AppColors.verdeOscuro, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        const Text('Balance Financiero de Hoy', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal)),
                        Text('${hoy.day}/${hoy.month}/${hoy.year} · Resumen monetario del día', style: const TextStyle(fontSize: 12, color: AppColors.textoSecundario)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
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
                          _formatoMoneda.format(totalFacturado),
                          style: const TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w900),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(8)),
                          child: Text('$totalTransacciones ventas', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
                        ),
                      ],
                    ),
                    const Divider(color: Colors.white24, height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Text('Ticket promedio:', style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 12)),
                        Text(_formatoMoneda.format(ticketMedio), style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: <Widget>[
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFFBEB),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFD97706).withValues(alpha: 0.25)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          const Row(
                            children: <Widget>[
                              Icon(Icons.hourglass_bottom_rounded, size: 16, color: Color(0xFFD97706)),
                              SizedBox(width: 6),
                              Text('Por cobrar', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Color(0xFFD97706))),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(
                            _formatoMoneda.format(datos?.montoCuotasPendientes ?? 0),
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: Color(0xFFD97706)),
                          ),
                          Text('${datos?.cuotasPendientes ?? 0} cuotas', style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF3E8FF),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFF7C3AED).withValues(alpha: 0.25)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          const Row(
                            children: <Widget>[
                              Icon(Icons.local_shipping_outlined, size: 16, color: Color(0xFF7C3AED)),
                              SizedBox(width: 6),
                              Text('Por entregar', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Color(0xFF7C3AED))),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(
                            '${datos?.ventasPorEntregar ?? 0}',
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: Color(0xFF7C3AED)),
                          ),
                          const Text('pedidos activos', style: TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              const Text('Desglose de cobros de hoy', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.textoPrincipal)),
              const SizedBox(height: 8),
              FutureBuilder<List<Venta>>(
                future: _ventasRepository.obtenerVentas(widget.sesion.token),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator(color: AppColors.verdeOscuro, strokeWidth: 2)));
                  }
                  final lista = snapshot.data ?? <Venta>[];
                  final ventasHoy = lista.where((v) {
                    if (v.fechaVenta == null) return false;
                    final f = v.fechaVenta!;
                    return f.year == hoy.year && f.month == hoy.month && f.day == hoy.day;
                  }).toList();

                  double efectivo = 0.0;
                  double qr = 0.0;
                  double transferencia = 0.0;
                  for (final v in ventasHoy) {
                    for (final p in v.pagos) {
                      if (p.metodoPago == MetodoPago.efectivo) efectivo += p.monto;
                      if (p.metodoPago == MetodoPago.qr) qr += p.monto;
                      if (p.metodoPago == MetodoPago.transferencia) transferencia += p.monto;
                    }
                  }

                  return Column(
                    children: <Widget>[
                      _FilaMetodoPago(etiqueta: 'Efectivo', icono: Icons.money_rounded, monto: _formatoMoneda.format(efectivo), color: const Color(0xFF16A34A)),
                      const SizedBox(height: 6),
                      _FilaMetodoPago(etiqueta: 'QR Simple', icono: Icons.qr_code_rounded, monto: _formatoMoneda.format(qr), color: const Color(0xFF2563EB)),
                      const SizedBox(height: 6),
                      _FilaMetodoPago(etiqueta: 'Transferencia', icono: Icons.account_balance_rounded, monto: _formatoMoneda.format(transferencia), color: const Color(0xFF9333EA)),
                    ],
                  );
                },
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
        title: const Text('Mueblería Edén'),
        actions: <Widget>[
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
      body: RefreshIndicator(
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
              errorMensaje: _errorMensaje,
              formatoMoneda: _formatoMoneda,
              onReintentar: _cargarResumen,
              onTapVentas: _mostrarVentasHoy,
              onTapBalance: _mostrarBalanceHoy,
              onTapPorCobrar: _mostrarVentasPorCobrar,
              onTapPorEntregar: _mostrarVentasPorEntregar,
            ),
            const SizedBox(height: 24),
            const _TituloSeccion(texto: 'Accesos rápidos'),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  child: _TarjetaModulo(
                    icono: Icons.local_shipping_outlined,
                    titulo: 'Ventas y\nentregas',
                    colorFondo: const Color(0xFFE2F4E9),
                    colorAcento: const Color(0xFF1E7A3E),
                    onTap: _abrirVentas,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _TarjetaModulo(
                    icono: Icons.warning_amber_rounded,
                    titulo: 'Alertas de\nstock',
                    colorFondo: const Color(0xFFFEEEDB),
                    colorAcento: const Color(0xFFD97706),
                    onTap: _abrirAlertasStock,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _TarjetaModulo(
                    icono: Icons.bed_outlined,
                    titulo: 'Catálogo',
                    colorFondo: const Color(0xFFEFE6FA),
                    colorAcento: const Color(0xFF7C3AED),
                    onTap: _abrirCatalogo,
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
    required this.errorMensaje,
    required this.formatoMoneda,
    required this.onReintentar,
    this.onTapVentas,
    this.onTapBalance,
    this.onTapPorCobrar,
    this.onTapPorEntregar,
  });

  final _EstadoResumen estado;
  final DashboardResumen? resumen;
  final String? errorMensaje;
  final NumberFormat formatoMoneda;
  final VoidCallback onReintentar;
  final VoidCallback? onTapVentas;
  final VoidCallback? onTapBalance;
  final VoidCallback? onTapPorCobrar;
  final VoidCallback? onTapPorEntregar;

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
        return Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: <Widget>[
                const Icon(Icons.cloud_off_rounded, color: AppColors.error, size: 40),
                const SizedBox(height: 10),
                Text(
                  errorMensaje ?? 'No se pudo cargar el resumen.',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: AppColors.textoPrincipal),
                ),
                const SizedBox(height: 14),
                OutlinedButton(onPressed: onReintentar, child: const Text('Reintentar')),
              ],
            ),
          ),
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
        final datos = resumen!;
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
                        valor: '${datos.totalVentasHoy}',
                        etiqueta: 'Ventas',
                        onTap: onTapVentas,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _MetricaResumen(
                        icono: Icons.payments_outlined,
                        valor: formatoMoneda.format(datos.montoVentasHoy),
                        etiqueta: 'Monto',
                        onTap: onTapBalance,
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
                        valor: formatoMoneda.format(datos.montoCuotasPendientes),
                        etiqueta: 'Por cobrar (${datos.cuotasPendientes})',
                        onTap: onTapPorCobrar,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _MetricaResumen(
                        icono: Icons.local_shipping_outlined,
                        valor: '${datos.ventasPorEntregar}',
                        etiqueta: 'Por entregar',
                        onTap: onTapPorEntregar,
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
}

class _MetricaResumen extends StatelessWidget {
  const _MetricaResumen({required this.icono, required this.valor, required this.etiqueta, this.onTap});

  final IconData icono;
  final String valor;
  final String etiqueta;
  final VoidCallback? onTap;

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
            color: const Color(0xFFE8F4EC),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
          ),
          child: Column(
            children: <Widget>[
              Icon(icono, color: AppColors.verdeOscuro, size: 20),
              const SizedBox(height: 6),
              Text(
                valor,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w800,
                  color: AppColors.textoPrincipal,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                etiqueta,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario),
              ),
              const SizedBox(height: 4),
              if (onTap != null)
                Text(
                  'Ver detalle ›',
                  style: TextStyle(fontSize: 9.5, fontWeight: FontWeight.w700, color: AppColors.verdeOscuro.withValues(alpha: 0.6)),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TarjetaModulo extends StatelessWidget {
  const _TarjetaModulo({
    required this.icono,
    required this.titulo,
    required this.colorFondo,
    required this.colorAcento,
    required this.onTap,
  });

  final IconData icono;
  final String titulo;
  final Color colorFondo;
  final Color colorAcento;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        height: 110,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: colorFondo,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: colorAcento.withValues(alpha: 0.18)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Container(
              width: 32,
              height: 32,
              decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
              child: Icon(icono, color: colorAcento, size: 17),
            ),
            const Spacer(),
            Text(
              titulo,
              style: const TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
                color: AppColors.textoPrincipal,
                height: 1.2,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilaMetodoPago extends StatelessWidget {
  const _FilaMetodoPago({
    required this.etiqueta,
    required this.icono,
    required this.monto,
    required this.color,
  });

  final String etiqueta;
  final IconData icono;
  final String monto;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAF9),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.15)),
      ),
      child: Row(
        children: <Widget>[
          Icon(icono, size: 20, color: color),
          const SizedBox(width: 10),
          Text(etiqueta, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.textoPrincipal)),
          const Spacer(),
          Text(monto, style: TextStyle(fontSize: 13.5, fontWeight: FontWeight.bold, color: color)),
        ],
      ),
    );
  }
}
