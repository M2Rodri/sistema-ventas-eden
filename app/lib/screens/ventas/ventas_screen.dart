import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/ventas_repository.dart';
import '../../models/venta.dart';
import '../../theme/app_colors.dart';
import 'venta_detalle_screen.dart';

enum _EstadoVentas { cargando, conDatos, vacio, error }

enum _Filtro { todas, porCobrar, porEntregar }

/// Ventas y entregas: lista con filtros, mismo patrón que Catálogo y
/// Alertas de stock (repositorio propio, mismos cuatro estados).
class VentasScreen extends StatefulWidget {
  const VentasScreen({super.key, required this.token});

  final String token;

  @override
  State<VentasScreen> createState() => _VentasScreenState();
}

class _VentasScreenState extends State<VentasScreen> {
  final _ventasRepository = VentasRepository();
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);
  final _formatoFecha = DateFormat('dd/MM/yyyy HH:mm');

  _EstadoVentas _estado = _EstadoVentas.cargando;
  _Filtro _filtro = _Filtro.todas;
  List<Venta> _ventas = <Venta>[];
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _cargarVentas();
  }

  Future<void> _cargarVentas() async {
    setState(() {
      _estado = _EstadoVentas.cargando;
      _errorMensaje = null;
    });

    try {
      final ventas = await _ventasRepository.obtenerVentas(widget.token);
      if (!mounted) return;
      setState(() {
        _ventas = ventas;
        _estado = ventas.isEmpty ? _EstadoVentas.vacio : _EstadoVentas.conDatos;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = error.mensaje;
        _estado = _EstadoVentas.error;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = 'No se pudieron cargar las ventas.';
        _estado = _EstadoVentas.error;
      });
    }
  }

  List<Venta> get _filtradas {
    switch (_filtro) {
      case _Filtro.todas:
        return _ventas;
      case _Filtro.porCobrar:
        return _ventas.where((v) => v.tieneSaldoPendiente).toList();
      case _Filtro.porEntregar:
        return _ventas.where((v) => v.estadoEntrega == EstadoEntrega.pendiente).toList();
    }
  }

  Future<void> _abrirDetalle(Venta venta) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => VentaDetalleScreen(idVenta: venta.id, token: widget.token),
      ),
    );
    // Al volver del detalle puede haber cambiado el saldo o la entrega
    // (se registró un pago, se marcó como entregada): se recarga siempre,
    // es más simple y seguro que tratar de adivinar si hizo falta.
    if (mounted) _cargarVentas();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ventas y entregas')),
      body: RefreshIndicator(
        color: AppColors.verdeOscuro,
        onRefresh: _cargarVentas,
        child: _estado == _EstadoVentas.cargando
            ? const _CentroCargando()
            : _estado == _EstadoVentas.error
                ? _CentroError(mensaje: _errorMensaje, onReintentar: _cargarVentas)
                : _estado == _EstadoVentas.vacio
                    ? const _CentroVacio(mensaje: 'Todavía no hay ventas registradas.')
                    : Column(
                        children: <Widget>[
                          Padding(
                            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                            child: _SelectorFiltro(
                              filtro: _filtro,
                              onCambiar: (f) => setState(() => _filtro = f),
                            ),
                          ),
                          Expanded(
                            child: _filtradas.isEmpty
                                ? const _CentroVacio(mensaje: 'Ninguna venta coincide con este filtro.')
                                : ListView.separated(
                                    physics: const AlwaysScrollableScrollPhysics(),
                                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 20),
                                    itemCount: _filtradas.length,
                                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                                    itemBuilder: (context, index) {
                                      final venta = _filtradas[index];
                                      return _TarjetaVenta(
                                        venta: venta,
                                        formatoMoneda: _formatoMoneda,
                                        formatoFecha: _formatoFecha,
                                        onTap: () => _abrirDetalle(venta),
                                      );
                                    },
                                  ),
                          ),
                        ],
                      ),
      ),
    );
  }
}

class _SelectorFiltro extends StatelessWidget {
  const _SelectorFiltro({required this.filtro, required this.onCambiar});

  final _Filtro filtro;
  final ValueChanged<_Filtro> onCambiar;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Expanded(child: _ChipFiltro(texto: 'Todas', seleccionado: filtro == _Filtro.todas, onTap: () => onCambiar(_Filtro.todas))),
        const SizedBox(width: 8),
        Expanded(child: _ChipFiltro(texto: 'Por cobrar', seleccionado: filtro == _Filtro.porCobrar, onTap: () => onCambiar(_Filtro.porCobrar))),
        const SizedBox(width: 8),
        Expanded(child: _ChipFiltro(texto: 'Por entregar', seleccionado: filtro == _Filtro.porEntregar, onTap: () => onCambiar(_Filtro.porEntregar))),
      ],
    );
  }
}

class _ChipFiltro extends StatelessWidget {
  const _ChipFiltro({required this.texto, required this.seleccionado, required this.onTap});

  final String texto;
  final bool seleccionado;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 9),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: seleccionado ? AppColors.verdeOscuro : Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: seleccionado ? AppColors.verdeOscuro : Colors.grey.shade300),
        ),
        child: Text(
          texto,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w700,
            color: seleccionado ? Colors.white : AppColors.textoSecundario,
          ),
        ),
      ),
    );
  }
}

class _CentroCargando extends StatelessWidget {
  const _CentroCargando();

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: const <Widget>[
        SizedBox(height: 120),
        Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro)),
      ],
    );
  }
}

class _CentroVacio extends StatelessWidget {
  const _CentroVacio({required this.mensaje});

  final String mensaje;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: <Widget>[
        const SizedBox(height: 80),
        const Icon(Icons.receipt_long_outlined, color: AppColors.textoSecundario, size: 40),
        const SizedBox(height: 10),
        Text(mensaje, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textoSecundario)),
      ],
    );
  }
}

class _CentroError extends StatelessWidget {
  const _CentroError({required this.mensaje, required this.onReintentar});

  final String? mensaje;
  final VoidCallback onReintentar;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: <Widget>[
        const SizedBox(height: 100),
        const Icon(Icons.cloud_off_rounded, color: AppColors.error, size: 40),
        const SizedBox(height: 10),
        Text(
          mensaje ?? 'No se pudieron cargar las ventas.',
          textAlign: TextAlign.center,
          style: const TextStyle(color: AppColors.textoPrincipal),
        ),
        const SizedBox(height: 14),
        Center(child: OutlinedButton(onPressed: onReintentar, child: const Text('Reintentar'))),
      ],
    );
  }
}

class _TarjetaVenta extends StatelessWidget {
  const _TarjetaVenta({
    required this.venta,
    required this.formatoMoneda,
    required this.formatoFecha,
    required this.onTap,
  });

  final Venta venta;
  final NumberFormat formatoMoneda;
  final DateFormat formatoFecha;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
          boxShadow: <BoxShadow>[
            BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 3)),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Expanded(
                  child: Text(
                    venta.nombreCliente,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14.5),
                  ),
                ),
                Text(
                  formatoMoneda.format(venta.montoTotal),
                  style: const TextStyle(color: AppColors.verdeOscuro, fontWeight: FontWeight.w900, fontSize: 14.5),
                ),
              ],
            ),
            if (venta.fechaVenta != null) ...<Widget>[
              const SizedBox(height: 2),
              Text(formatoFecha.format(venta.fechaVenta!), style: const TextStyle(fontSize: 11.5, color: AppColors.textoSecundario)),
            ],
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: <Widget>[
                if (venta.estado == EstadoVenta.cancelada)
                  const _Badge(texto: 'Cancelada', color: AppColors.error)
                else if (venta.tieneSaldoPendiente)
                  _Badge(texto: 'Saldo: ${formatoMoneda.format(venta.saldoPendiente)}', color: const Color(0xFFD97706))
                else
                  const _Badge(texto: 'Pagada', color: AppColors.verdeOscuro),
                _Badge(
                  texto: venta.estadoEntrega == EstadoEntrega.entregado ? 'Entregado' : 'Por entregar',
                  color: venta.estadoEntrega == EstadoEntrega.entregado ? AppColors.verdeSuave : const Color(0xFF7C3AED),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.texto, required this.color});

  final String texto;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(8)),
      child: Text(texto, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: color)),
    );
  }
}
