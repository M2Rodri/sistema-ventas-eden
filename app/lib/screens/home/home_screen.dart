import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/auth_repository.dart';
import '../../data/dashboard_repository.dart';
import '../../models/dashboard_resumen.dart';
import '../../models/sesion.dart';
import '../../theme/app_colors.dart';
import '../catalogo/catalogo_screen.dart';
import '../login/login_screen.dart';

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
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);

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

  void _proximamente(String modulo) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$modulo: todavía no está disponible en esta versión.')),
    );
  }

  void _abrirCatalogo() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => CatalogoScreen(token: widget.sesion.token)),
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
        backgroundColor: AppColors.verdeOscuro,
        onPressed: () => _proximamente('Nueva venta'),
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
                    onTap: () => _proximamente('Ventas y entregas'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _TarjetaModulo(
                    icono: Icons.warning_amber_rounded,
                    titulo: 'Alertas de\nstock',
                    colorFondo: const Color(0xFFFEEEDB),
                    colorAcento: const Color(0xFFD97706),
                    onTap: () => _proximamente('Alertas de stock'),
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
  });

  final _EstadoResumen estado;
  final DashboardResumen? resumen;
  final String? errorMensaje;
  final NumberFormat formatoMoneda;
  final VoidCallback onReintentar;

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
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _MetricaResumen(
                        icono: Icons.payments_outlined,
                        valor: formatoMoneda.format(datos.montoVentasHoy),
                        etiqueta: 'Monto',
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
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _MetricaResumen(
                        icono: Icons.local_shipping_outlined,
                        valor: '${datos.ventasPorEntregar}',
                        etiqueta: 'Por entregar',
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
  const _MetricaResumen({required this.icono, required this.valor, required this.etiqueta});

  final IconData icono;
  final String valor;
  final String etiqueta;

  @override
  Widget build(BuildContext context) {
    return Container(
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
        ],
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
