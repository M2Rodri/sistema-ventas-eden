import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/ventas_repository.dart';
import '../../models/venta.dart';
import '../../theme/app_colors.dart';
import 'estado_entrega_ui.dart';

enum _EstadoDetalle { cargando, conDatos, error }

/// Detalle de una venta: productos, pagos, entrega y sus acciones: registrar
/// pago y marcar entregado (ADMIN y EMPLEADO); corregir a pendiente y editar los
/// datos de entrega (solo ADMIN).
class VentaDetalleScreen extends StatefulWidget {
  const VentaDetalleScreen({super.key, required this.idVenta, required this.token, this.esAdmin = false});

  final int idVenta;
  final String token;

  /// Corregir a pendiente y editar los datos de entrega son solo del ADMIN.
  final bool esAdmin;

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

  /// Corre una acción de entrega que devuelve la venta actualizada, avisa el
  /// resultado y recarga el detalle.
  Future<void> _ejecutarAccionEntrega(
    Future<void> Function() accion, {
    required String mensajeOk,
    required String mensajeError,
  }) async {
    setState(() => _accionEnCurso = true);
    try {
      await accion();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensajeOk)));
      await _cargarVenta();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.mensaje)));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensajeError)));
    } finally {
      if (mounted) setState(() => _accionEnCurso = false);
    }
  }

  /// Entregar funciona igual con o sin saldo pendiente, sin avisos.
  Future<void> _marcarEntregado() {
    return _ejecutarAccionEntrega(
      () => _ventasRepository.marcarEntregado(widget.idVenta, widget.token),
      mensajeOk: 'Venta marcada como entregada',
      mensajeError: 'No se pudo marcar la entrega.',
    );
  }

  /// Corregir una entrega marcada por error (solo ADMIN): ENTREGADO -> PENDIENTE.
  /// Se llega tocando la etiqueta del estado, con confirmación; queda en la
  /// auditoría del backend.
  Future<void> _corregirAPendiente() async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: const Text('Corregir a Pendiente'),
        content: Text('La venta #${widget.idVenta} volverá a Pendiente de entrega. Queda registrado en la auditoría.'),
        actions: <Widget>[
          TextButton(onPressed: () => Navigator.of(contexto).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(contexto).pop(true), child: const Text('Corregir')),
        ],
      ),
    );
    if (confirmado != true) return;
    await _ejecutarAccionEntrega(
      () => _ventasRepository.corregirAPendiente(widget.idVenta, widget.token),
      mensajeOk: 'La venta volvió a Pendiente',
      mensajeError: 'No se pudo corregir la entrega.',
    );
  }

  /// Completar la dirección, la transportadora y la guía (solo ADMIN): el
  /// dueño despacha estando en la transportadora y ahí recibe la guía.
  Future<void> _editarEntrega() async {
    final venta = _venta;
    if (venta == null) return;

    final guardado = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _FormularioEntregaSheet(
        venta: venta,
        onGuardar: (direccion, transportadora, guia) async {
          await _ventasRepository.actualizarDatosEntrega(
            venta.id,
            direccionDestino: direccion,
            transportadora: transportadora,
            guiaRemision: guia,
            token: widget.token,
          );
        },
      ),
    );

    if (guardado == true) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Datos de entrega guardados')),
        );
      }
      _cargarVenta();
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
            esAdmin: widget.esAdmin,
            onRegistrarPago: _abrirFormularioPago,
            onMarcarEntregado: _marcarEntregado,
            onCorregirEntrega: _corregirAPendiente,
            onEditarEntrega: _editarEntrega,
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
    required this.esAdmin,
    required this.onRegistrarPago,
    required this.onMarcarEntregado,
    required this.onCorregirEntrega,
    required this.onEditarEntrega,
  });

  final Venta venta;
  final NumberFormat formatoMoneda;
  final DateFormat formatoFecha;
  final bool accionEnCurso;
  final bool esAdmin;
  final VoidCallback onRegistrarPago;
  final VoidCallback onMarcarEntregado;
  final VoidCallback onCorregirEntrega;
  final VoidCallback onEditarEntrega;

  String get _modalidadTexto {
    switch (venta.modalidadEntrega) {
      case ModalidadEntrega.retiro:
        return 'En tienda';
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
              const SizedBox(height: 4),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Text(
                    cancelada ? 'Pagado antes de anular' : 'Total pagado',
                    style: const TextStyle(color: AppColors.textoSecundario),
                  ),
                  Text(
                    formatoMoneda.format(_totalPagado(venta, cancelada)),
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
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
                const _Badge(texto: 'Anulada', color: AppColors.error),
              ] else ...<Widget>[
                const SizedBox(height: 6),
                const _Badge(texto: 'Completada', color: AppColors.verdeOscuro),
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
                if (venta.direccionDestino != null && venta.direccionDestino!.isNotEmpty)
                  Text(venta.direccionDestino!, style: const TextStyle(color: AppColors.textoSecundario)),
                if (venta.ciudad != null && venta.ciudad!.isNotEmpty)
                  Text(venta.ciudad!, style: const TextStyle(color: AppColors.textoSecundario)),
                if (venta.modalidadEntrega == ModalidadEntrega.transportadora) ...<Widget>[
                  Text(
                    'Transportadora: ${(venta.transportadora != null && venta.transportadora!.isNotEmpty) ? venta.transportadora : 'Sin completar'}',
                    style: const TextStyle(color: AppColors.textoSecundario),
                  ),
                  Text(
                    'Guía: ${(venta.guiaRemision != null && venta.guiaRemision!.isNotEmpty) ? venta.guiaRemision : 'Sin completar'}',
                    style: const TextStyle(color: AppColors.textoSecundario),
                  ),
                ],
              ],
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: <Widget>[
                  _Badge(
                    texto: venta.estadoEntrega.etiqueta,
                    color: colorEstadoEntrega(venta.estadoEntrega),
                    // Solo ADMIN, en una venta entregada que no es en tienda.
                    onTap: (corregirEntregaActivo &&
                            esAdmin &&
                            !accionEnCurso &&
                            !cancelada &&
                            venta.modalidadEntrega != ModalidadEntrega.retiro &&
                            venta.estadoEntrega == EstadoEntrega.entregado)
                        ? onCorregirEntrega
                        : null,
                  ),
                  if (venta.faltaCompletarEnvio) const _Badge(texto: 'Falta completar', color: Color(0xFFEA580C)),
                ],
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
          // Entregar: ADMIN y EMPLEADO. El saldo pendiente no la bloquea.
          if (!yaEntregado) ...<Widget>[
            if (venta.tieneSaldoPendiente) const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: accionEnCurso ? null : onMarcarEntregado,
              icon: accionEnCurso
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.check_circle_outline),
              label: const Text('Marcar como entregada'),
            ),
          ],
          // Editar datos de entrega: solo ADMIN, en domicilio y transportadora.
          if (esAdmin && venta.modalidadEntrega != ModalidadEntrega.retiro) ...<Widget>[
            const SizedBox(height: 10),
            TextButton.icon(
              onPressed: accionEnCurso ? null : onEditarEntrega,
              icon: const Icon(Icons.edit_outlined),
              label: const Text('Editar datos de entrega'),
            ),
          ],
        ],
      ],
    );
  }
}

/// Lo que ya se cobró. En una venta anulada el saldo queda en 0, así que no
/// sirve restarlo: se suman los pagos registrados (no se sabe si se devolvió).
double _totalPagado(Venta venta, bool cancelada) {
  final total = cancelada
      ? venta.pagos.fold<double>(0, (suma, pago) => suma + pago.monto)
      : venta.montoTotal - venta.saldoPendiente;
  return (total * 100).round() / 100;
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
  const _Badge({required this.texto, required this.color, this.onTap});

  final String texto;
  final Color color;

  /// Si viene, la etiqueta se puede tocar (por ejemplo, para corregir la entrega).
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final etiqueta = Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(8)),
      child: Text(texto, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: color)),
    );
    if (onTap == null) return etiqueta;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: etiqueta,
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

/// Completar o corregir la dirección, la transportadora y la guía de una
/// venta (solo ADMIN), en bottom sheet, con el mismo lenguaje visual que el de
/// registrar pago. Un campo vacío borra el dato.
class _FormularioEntregaSheet extends StatefulWidget {
  const _FormularioEntregaSheet({required this.venta, required this.onGuardar});

  final Venta venta;
  final Future<void> Function(String direccion, String transportadora, String guia) onGuardar;

  @override
  State<_FormularioEntregaSheet> createState() => _FormularioEntregaSheetState();
}

class _FormularioEntregaSheetState extends State<_FormularioEntregaSheet> {
  late final TextEditingController _direccionCtrl;
  late final TextEditingController _transportadoraCtrl;
  late final TextEditingController _guiaCtrl;
  bool _guardando = false;
  String? _error;

  bool get _esTransportadora => widget.venta.modalidadEntrega == ModalidadEntrega.transportadora;

  @override
  void initState() {
    super.initState();
    _direccionCtrl = TextEditingController(text: widget.venta.direccionDestino ?? '');
    _transportadoraCtrl = TextEditingController(text: widget.venta.transportadora ?? '');
    _guiaCtrl = TextEditingController(text: widget.venta.guiaRemision ?? '');
  }

  @override
  void dispose() {
    _direccionCtrl.dispose();
    _transportadoraCtrl.dispose();
    _guiaCtrl.dispose();
    super.dispose();
  }

  Future<void> _guardar() async {
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      await widget.onGuardar(
        _direccionCtrl.text.trim(),
        _transportadoraCtrl.text.trim(),
        _guiaCtrl.text.trim(),
      );
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      setState(() {
        _error = error.mensaje;
        _guardando = false;
      });
    } catch (_) {
      setState(() {
        _error = 'No se pudieron guardar los datos de entrega.';
        _guardando = false;
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
              const Text('Datos de entrega', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text(
                'Venta #${widget.venta.id}',
                style: const TextStyle(color: AppColors.textoSecundario, fontSize: 13),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _direccionCtrl,
                enabled: !_guardando,
                decoration: const InputDecoration(
                  labelText: 'Dirección',
                  helperText: 'Te sirve para coordinar la entrega',
                ),
              ),
              if (_esTransportadora) ...<Widget>[
                const SizedBox(height: 12),
                TextField(
                  controller: _transportadoraCtrl,
                  enabled: !_guardando,
                  decoration: const InputDecoration(labelText: 'Transportadora'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _guiaCtrl,
                  enabled: !_guardando,
                  decoration: const InputDecoration(labelText: 'Guía de remisión'),
                ),
              ],
              if (_error != null) ...<Widget>[
                const SizedBox(height: 10),
                Text(_error!, style: const TextStyle(color: AppColors.error, fontSize: 12.5)),
              ],
              const SizedBox(height: 18),
              FilledButton(
                onPressed: _guardando ? null : _guardar,
                child: _guardando
                    ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2.2, color: Colors.white))
                    : const Text('Guardar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
