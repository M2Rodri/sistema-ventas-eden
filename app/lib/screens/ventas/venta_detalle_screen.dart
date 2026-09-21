import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/ventas_repository.dart';
import '../../models/venta.dart';
import '../../theme/app_colors.dart';

enum _EstadoDetalle { cargando, conDatos, error }

/// Detalle de una venta: productos, pagos, entrega, y las dos acciones que
/// pide la pantalla (registrar pago, marcar entregado).
class VentaDetalleScreen extends StatefulWidget {
  const VentaDetalleScreen({super.key, required this.idVenta, required this.token});

  final int idVenta;
  final String token;

  @override
  State<VentaDetalleScreen> createState() => _VentaDetalleScreenState();
}

class _VentaDetalleScreenState extends State<VentaDetalleScreen> {
  final _ventasRepository = VentasRepository();
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);
  final _formatoFecha = DateFormat('dd/MM/yyyy HH:mm');

  _EstadoDetalle _estado = _EstadoDetalle.cargando;
  Venta? _venta;
  String? _errorMensaje;
  bool _accionEnCurso = false;

  @override
  void initState() {
    super.initState();
    _cargarVenta();
  }

  Future<void> _cargarVenta() async {
    setState(() {
      _estado = _EstadoDetalle.cargando;
      _errorMensaje = null;
    });

    try {
      final venta = await _ventasRepository.obtenerVenta(widget.idVenta, widget.token);
      if (!mounted) return;
      setState(() {
        _venta = venta;
        _estado = _EstadoDetalle.conDatos;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = error.mensaje;
        _estado = _EstadoDetalle.error;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = 'No se pudo cargar la venta.';
        _estado = _EstadoDetalle.error;
      });
    }
  }

  Future<void> _abrirFormularioPago() async {
    final venta = _venta;
    if (venta == null) return;

    final registrado = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _FormularioPagoSheet(
        saldoPendiente: venta.saldoPendiente,
        formatoMoneda: _formatoMoneda,
        onConfirmar: (monto, metodo, referencia) async {
          await _ventasRepository.registrarPago(
            idVenta: venta.id,
            monto: monto,
            metodoPago: metodo,
            referencia: referencia,
            token: widget.token,
          );
        },
      ),
    );

    if (registrado == true) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Pago registrado')),
        );
      }
      _cargarVenta();
    }
  }

  Future<void> _marcarEntregado() async {
    setState(() => _accionEnCurso = true);
    try {
      await _ventasRepository.marcarEntregado(widget.idVenta, widget.token);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Venta marcada como entregada')),
      );
      await _cargarVenta();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.mensaje)));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo marcar la entrega.')),
      );
    } finally {
      if (mounted) setState(() => _accionEnCurso = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_venta != null ? 'Venta #${_venta!.id}' : 'Venta')),
      body: switch (_estado) {
        _EstadoDetalle.cargando => const Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro)),
        _EstadoDetalle.error => _CentroError(mensaje: _errorMensaje, onReintentar: _cargarVenta),
        _EstadoDetalle.conDatos => _Contenido(
            venta: _venta!,
            formatoMoneda: _formatoMoneda,
            formatoFecha: _formatoFecha,
            accionEnCurso: _accionEnCurso,
            onRegistrarPago: _abrirFormularioPago,
            onMarcarEntregado: _marcarEntregado,
          ),
      },
    );
  }
}

class _CentroError extends StatelessWidget {
  const _CentroError({required this.mensaje, required this.onReintentar});

  final String? mensaje;
  final VoidCallback onReintentar;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            const Icon(Icons.cloud_off_rounded, color: AppColors.error, size: 40),
            const SizedBox(height: 10),
            Text(
              mensaje ?? 'No se pudo cargar la venta.',
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.textoPrincipal),
            ),
            const SizedBox(height: 14),
            OutlinedButton(onPressed: onReintentar, child: const Text('Reintentar')),
          ],
        ),
      ),
    );
  }
}

class _Contenido extends StatelessWidget {
  const _Contenido({
    required this.venta,
    required this.formatoMoneda,
    required this.formatoFecha,
    required this.accionEnCurso,
    required this.onRegistrarPago,
    required this.onMarcarEntregado,
  });

  final Venta venta;
  final NumberFormat formatoMoneda;
  final DateFormat formatoFecha;
  final bool accionEnCurso;
  final VoidCallback onRegistrarPago;
  final VoidCallback onMarcarEntregado;

  String get _modalidadTexto {
    switch (venta.modalidadEntrega) {
      case ModalidadEntrega.retiro:
        return 'Retiro en el local';
      case ModalidadEntrega.domicilio:
        return 'Envío a domicilio';
      case ModalidadEntrega.transportadora:
        return 'Envío por transportadora';
    }
  }

  @override
  Widget build(BuildContext context) {
    final yaEntregado = venta.estadoEntrega == EstadoEntrega.entregado;
    final cancelada = venta.estado == EstadoVenta.cancelada;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
      children: <Widget>[
        _Seccion(
          titulo: 'Cliente',
          icono: Icons.person_outline,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(venta.nombreCliente, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
              if (venta.telefonoCliente != null && venta.telefonoCliente!.isNotEmpty)
                Text(venta.telefonoCliente!, style: const TextStyle(color: AppColors.textoSecundario)),
              if (venta.fechaVenta != null) ...<Widget>[
                const SizedBox(height: 4),
                Text(formatoFecha.format(venta.fechaVenta!), style: const TextStyle(fontSize: 12, color: AppColors.textoSecundario)),
              ],
            ],
          ),
        ),
        const SizedBox(height: 12),
        _Seccion(
          titulo: 'Importes',
          icono: Icons.payments_outlined,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  const Text('Total', style: TextStyle(color: AppColors.textoSecundario)),
                  Text(formatoMoneda.format(venta.montoTotal), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16, color: AppColors.verdeOscuro)),
                ],
              ),
              if (venta.tieneSaldoPendiente) ...<Widget>[
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    const Text('Saldo pendiente', style: TextStyle(color: AppColors.textoSecundario)),
                    Text(
                      formatoMoneda.format(venta.saldoPendiente),
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFFD97706)),
                    ),
                  ],
                ),
              ] else if (cancelada) ...<Widget>[
                const SizedBox(height: 6),
                const _Badge(texto: 'Cancelada', color: AppColors.error),
              ] else ...<Widget>[
                const SizedBox(height: 6),
                const _Badge(texto: 'Pagada', color: AppColors.verdeOscuro),
              ],
            ],
          ),
        ),
        const SizedBox(height: 12),
        _Seccion(
          titulo: 'Entrega',
          icono: Icons.local_shipping_outlined,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(_modalidadTexto, style: const TextStyle(fontWeight: FontWeight.w600)),
              if (venta.modalidadEntrega != ModalidadEntrega.retiro) ...<Widget>[
                const SizedBox(height: 4),
                if (venta.direccionDestino != null) Text(venta.direccionDestino!, style: const TextStyle(color: AppColors.textoSecundario)),
                if (venta.ciudad != null) Text(venta.ciudad!, style: const TextStyle(color: AppColors.textoSecundario)),
                if (venta.transportadora != null && venta.transportadora!.isNotEmpty)
                  Text('Transportadora: ${venta.transportadora}', style: const TextStyle(color: AppColors.textoSecundario)),
                if (venta.guiaRemision != null && venta.guiaRemision!.isNotEmpty)
                  Text('Guía: ${venta.guiaRemision}', style: const TextStyle(color: AppColors.textoSecundario)),
              ],
              const SizedBox(height: 8),
              _Badge(
                texto: yaEntregado ? 'Entregado' : 'Pendiente de entrega',
                color: yaEntregado ? AppColors.verdeSuave : const Color(0xFF7C3AED),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _Seccion(
          titulo: 'Productos',
          icono: Icons.bed_outlined,
          child: Column(
            children: venta.detalles.map((d) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: Text(
                        '${d.cantidad}× ${d.nombreProducto}',
                        style: const TextStyle(fontSize: 13),
                      ),
                    ),
                    Text(formatoMoneda.format(d.subtotal), style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700)),
                  ],
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 12),
        _Seccion(
          titulo: 'Pagos',
          icono: Icons.credit_card_outlined,
          child: venta.pagos.isEmpty
              ? const Text('Todavía no hay pagos registrados.', style: TextStyle(color: AppColors.textoSecundario))
              : Column(
                  children: venta.pagos.map((p) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Text(p.metodoPago?.etiqueta ?? '—', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                                if (p.fechaPago != null)
                                  Text(
                                    formatoFecha.format(p.fechaPago!) +
                                        (p.nombreUsuario != null ? ' · Registrado por ${p.nombreUsuario}' : ''),
                                    style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario),
                                  ),
                              ],
                            ),
                          ),
                          Text(formatoMoneda.format(p.monto), style: const TextStyle(fontWeight: FontWeight.w700, color: AppColors.verdeOscuro)),
                        ],
                      ),
                    );
                  }).toList(),
                ),
        ),
        const SizedBox(height: 20),
        if (!cancelada) ...<Widget>[
          if (venta.tieneSaldoPendiente)
            FilledButton.icon(
              onPressed: accionEnCurso ? null : onRegistrarPago,
              icon: const Icon(Icons.add_card_outlined),
              label: const Text('Registrar pago'),
            ),
          if (venta.tieneSaldoPendiente && !yaEntregado) const SizedBox(height: 10),
          if (!yaEntregado)
            OutlinedButton.icon(
              onPressed: accionEnCurso || venta.tieneSaldoPendiente ? null : onMarcarEntregado,
              icon: accionEnCurso
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.check_circle_outline),
              label: Text(venta.tieneSaldoPendiente ? 'No se puede entregar con saldo pendiente' : 'Marcar como entregada'),
            ),
        ],
      ],
    );
  }
}

class _Seccion extends StatelessWidget {
  const _Seccion({required this.titulo, required this.icono, required this.child});

  final String titulo;
  final IconData icono;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Icon(icono, size: 17, color: AppColors.verdeOscuro),
              const SizedBox(width: 6),
              Text(titulo, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.textoPrincipal)),
            ],
          ),
          const SizedBox(height: 8),
          child,
        ],
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

/// Formulario de "registrar pago", en bottom sheet (mismo lenguaje visual
/// que el detalle de producto de Catálogo).
class _FormularioPagoSheet extends StatefulWidget {
  const _FormularioPagoSheet({
    required this.saldoPendiente,
    required this.formatoMoneda,
    required this.onConfirmar,
  });

  final double saldoPendiente;
  final NumberFormat formatoMoneda;
  final Future<void> Function(double monto, MetodoPago metodo, String? referencia) onConfirmar;

  @override
  State<_FormularioPagoSheet> createState() => _FormularioPagoSheetState();
}

class _FormularioPagoSheetState extends State<_FormularioPagoSheet> {
  late final TextEditingController _montoCtrl;
  final _referenciaCtrl = TextEditingController();
  MetodoPago _metodo = MetodoPago.efectivo;
  bool _enviando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _montoCtrl = TextEditingController(text: widget.saldoPendiente.toStringAsFixed(2));
  }

  @override
  void dispose() {
    _montoCtrl.dispose();
    _referenciaCtrl.dispose();
    super.dispose();
  }

  Future<void> _confirmar() async {
    final monto = double.tryParse(_montoCtrl.text.replaceAll(',', '.'));
    if (monto == null || monto <= 0) {
      setState(() => _error = 'Ingresá un monto válido.');
      return;
    }
    if (monto > widget.saldoPendiente) {
      setState(() => _error = 'El monto no puede superar el saldo pendiente.');
      return;
    }

    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      await widget.onConfirmar(monto, _metodo, _referenciaCtrl.text.trim());
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      setState(() {
        _error = error.mensaje;
        _enviando = false;
      });
    } catch (_) {
      setState(() {
        _error = 'No se pudo registrar el pago.';
        _enviando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SafeArea(
        child: Container(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: Colors.grey.shade300, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              const SizedBox(height: 16),
              const Text('Registrar pago', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text(
                'Saldo pendiente: ${widget.formatoMoneda.format(widget.saldoPendiente)}',
                style: const TextStyle(color: AppColors.textoSecundario, fontSize: 13),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _montoCtrl,
                enabled: !_enviando,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'Monto a pagar (Bs.)'),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                children: MetodoPago.values.map((m) {
                  final sel = _metodo == m;
                  return SizedBox(
                    height: 48,
                    child: ChoiceChip(
                      label: Text(m.etiqueta),
                      selected: sel,
                      onSelected: _enviando ? null : (_) => setState(() => _metodo = m),
                      selectedColor: AppColors.verdeOscuro,
                      labelStyle: TextStyle(color: sel ? Colors.white : AppColors.textoPrincipal, fontWeight: FontWeight.w600),
                    ),
                  );
                }).toList(),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _referenciaCtrl,
                enabled: !_enviando,
                decoration: const InputDecoration(labelText: 'Referencia (opcional)'),
              ),
              if (_error != null) ...<Widget>[
                const SizedBox(height: 10),
                Text(_error!, style: const TextStyle(color: AppColors.error, fontSize: 12.5)),
              ],
              const SizedBox(height: 18),
              FilledButton(
                onPressed: _enviando ? null : _confirmar,
                child: _enviando
                    ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2.2, color: Colors.white))
                    : const Text('Confirmar pago'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
