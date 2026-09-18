import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/catalogo_repository.dart';
import '../../data/clientes_repository.dart';
import '../../data/ventas_repository.dart';
import '../../models/cliente.dart';
import '../../models/producto_catalogo.dart';
import '../../models/venta.dart';
import '../../theme/app_colors.dart';

enum _EstadoCarga { cargando, listo, error }

/// Nueva venta: mismos campos, mismas validaciones y mismo endpoint que
/// RegistrarVentaModal.tsx (frontend web). La disposición en secciones
/// numeradas, los chips y el selector de catálogo en bottom sheet están
/// tomados de record_form_screen.dart (Módulo 3).
class NuevaVentaScreen extends StatefulWidget {
  const NuevaVentaScreen({super.key, required this.token});

  final String token;

  @override
  State<NuevaVentaScreen> createState() => _NuevaVentaScreenState();
}

class _NuevaVentaScreenState extends State<NuevaVentaScreen> {
  final _clientesRepository = ClientesRepository();
  final _catalogoRepository = CatalogoRepository();
  final _ventasRepository = VentasRepository();
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);

  _EstadoCarga _estado = _EstadoCarga.cargando;
  String? _errorCarga;
  List<Cliente> _clientes = <Cliente>[];
  List<ProductoCatalogo> _catalogo = <ProductoCatalogo>[];

  // Cliente
  Cliente? _clienteSeleccionado;
  final _nombreClienteCtrl = TextEditingController();
  final _telefonoClienteCtrl = TextEditingController();

  // Productos
  final List<ItemCarrito> _carrito = <ItemCarrito>[];

  // Pago
  MetodoPago _metodo = MetodoPago.efectivo;
  bool _ventaACredito = false;
  final _saldoCtrl = TextEditingController();
  File? _comprobante;

  // Entrega
  ModalidadEntrega _modalidad = ModalidadEntrega.retiro;
  final _direccionCtrl = TextEditingController();
  final _ciudadCtrl = TextEditingController();
  final _transportadoraCtrl = TextEditingController();
  final _guiaCtrl = TextEditingController();

  bool _enviando = false;
  String? _errorEnvio;

  @override
  void initState() {
    super.initState();
    _cargarDatos();
  }

  @override
  void dispose() {
    _nombreClienteCtrl.dispose();
    _telefonoClienteCtrl.dispose();
    _saldoCtrl.dispose();
    _direccionCtrl.dispose();
    _ciudadCtrl.dispose();
    _transportadoraCtrl.dispose();
    _guiaCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarDatos() async {
    setState(() {
      _estado = _EstadoCarga.cargando;
      _errorCarga = null;
    });
    try {
      final resultados = await Future.wait(<Future<Object>>[
        _clientesRepository.obtenerClientes(widget.token),
        _catalogoRepository.obtenerCatalogo(widget.token),
      ]);
      if (!mounted) return;
      setState(() {
        _clientes = resultados[0] as List<Cliente>;
        _catalogo = resultados[1] as List<ProductoCatalogo>;
        _estado = _EstadoCarga.listo;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorCarga = error.mensaje;
        _estado = _EstadoCarga.error;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorCarga = 'No se pudieron cargar los clientes y productos.';
        _estado = _EstadoCarga.error;
      });
    }
  }

  double get _totalVenta => _carrito.fold(0.0, (acc, item) => acc + item.subtotal);

  double get _montoPagado {
    if (!_ventaACredito) return _totalVenta;
    final saldo = double.tryParse(_saldoCtrl.text.replaceAll(',', '.')) ?? 0;
    final saldoAjustado = saldo.clamp(0, _totalVenta);
    return (_totalVenta - saldoAjustado).clamp(0, _totalVenta).toDouble();
  }

  void _agregarProducto(ProductoCatalogo producto) {
    final indice = _carrito.indexWhere((i) => i.idProducto == producto.id);
    if (indice != -1) {
      final actual = _carrito[indice];
      if (actual.cantidad >= actual.stockDisponible) return;
      setState(() => _carrito[indice] = actual.copyWith(cantidad: actual.cantidad + 1));
    } else {
      setState(() {
        _carrito.add(ItemCarrito(
          idProducto: producto.id,
          nombre: producto.nombre,
          skuProducto: producto.sku,
          precioOriginal: producto.precioVenta,
          precioFinal: producto.precioVenta,
          cantidad: 1,
          stockDisponible: producto.cantidadDisponible,
        ));
      });
    }
  }

  Future<void> _elegirProducto() async {
    final producto = await showModalBottomSheet<ProductoCatalogo>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _SelectorProductoSheet(catalogo: _catalogo),
    );
    if (producto != null) _agregarProducto(producto);
  }

  Future<void> _elegirCliente() async {
    final cliente = await showModalBottomSheet<Cliente>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _SelectorClienteSheet(clientes: _clientes),
    );
    if (cliente != null) {
      setState(() {
        _clienteSeleccionado = cliente;
        _nombreClienteCtrl.clear();
        _telefonoClienteCtrl.clear();
      });
    }
  }

  Future<void> _elegirComprobante(ImageSource origen) async {
    final picker = ImagePicker();
    final archivo = await picker.pickImage(source: origen, imageQuality: 85);
    if (archivo != null) setState(() => _comprobante = File(archivo.path));
  }

  String? _validar() {
    if (_clienteSeleccionado == null && _nombreClienteCtrl.text.trim().isEmpty) {
      return 'El nombre del cliente es obligatorio';
    }
    if (_carrito.isEmpty) {
      return 'Debe agregar al menos un producto';
    }
    if (_carrito.any((item) => item.cantidad < 1)) {
      return 'Hay un producto con cantidad inválida';
    }
    if (_montoPagado > _totalVenta) {
      return 'El monto pagado no puede superar el total de la venta';
    }
    if (_modalidad != ModalidadEntrega.retiro) {
      if (_direccionCtrl.text.trim().isEmpty || _ciudadCtrl.text.trim().isEmpty) {
        return 'La dirección y la ciudad son obligatorias para esta modalidad de entrega';
      }
    }
    if (_modalidad == ModalidadEntrega.transportadora) {
      if (_transportadoraCtrl.text.trim().isEmpty || _guiaCtrl.text.trim().isEmpty) {
        return 'La transportadora y la guía de remisión son obligatorias para esta modalidad de entrega';
      }
    }
    return null;
  }

  Future<void> _registrarVenta() async {
    final error = _validar();
    if (error != null) {
      setState(() => _errorEnvio = error);
      return;
    }

    setState(() {
      _enviando = true;
      _errorEnvio = null;
    });

    try {
      final request = NuevaVentaRequest(
        idCliente: _clienteSeleccionado?.id,
        nombreClienteInvitado: _clienteSeleccionado == null ? _nombreClienteCtrl.text.trim() : null,
        telefonoClienteInvitado: _clienteSeleccionado == null ? _telefonoClienteCtrl.text.trim() : null,
        metodoPago: _metodo,
        items: _carrito,
        montoPagado: _montoPagado,
        modalidadEntrega: _modalidad,
        direccionDestino: _modalidad != ModalidadEntrega.retiro ? _direccionCtrl.text.trim() : null,
        ciudad: _modalidad != ModalidadEntrega.retiro ? _ciudadCtrl.text.trim() : null,
        transportadora: _modalidad == ModalidadEntrega.transportadora ? _transportadoraCtrl.text.trim() : null,
        guiaRemision: _modalidad == ModalidadEntrega.transportadora ? _guiaCtrl.text.trim() : null,
      );

      final creada = await _ventasRepository.crearVenta(request, widget.token);

      // Igual que el formulario web: la venta ya quedó registrada aunque
      // esto falle, así que un error acá no revierte nada, solo avisa.
      if (_comprobante != null && creada.pagos.isNotEmpty) {
        try {
          await _ventasRepository.adjuntarComprobante(creada.pagos.first.id, _comprobante!, widget.token);
        } catch (_) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('La venta se registró, pero no se pudo subir el comprobante.'),
              ),
            );
          }
        }
      }

      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      setState(() {
        _errorEnvio = error.mensaje;
        _enviando = false;
      });
    } catch (_) {
      setState(() {
        _errorEnvio = 'No se pudo registrar la venta.';
        _enviando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nueva venta')),
      body: switch (_estado) {
        _EstadoCarga.cargando => const Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro)),
        _EstadoCarga.error => _CentroError(mensaje: _errorCarga, onReintentar: _cargarDatos),
        _EstadoCarga.listo => _Formulario(
            clienteSeleccionado: _clienteSeleccionado,
            nombreClienteCtrl: _nombreClienteCtrl,
            telefonoClienteCtrl: _telefonoClienteCtrl,
            onElegirCliente: _elegirCliente,
            onQuitarCliente: () => setState(() => _clienteSeleccionado = null),
            carrito: _carrito,
            formatoMoneda: _formatoMoneda,
            onAgregarProducto: _elegirProducto,
            onQuitarProducto: (id) => setState(() => _carrito.removeWhere((i) => i.idProducto == id)),
            onCambiarCantidad: (id, cantidad) {
              final i = _carrito.indexWhere((item) => item.idProducto == id);
              if (i != -1) setState(() => _carrito[i] = _carrito[i].copyWith(cantidad: cantidad));
            },
            onCambiarPrecio: (id, precio) {
              final i = _carrito.indexWhere((item) => item.idProducto == id);
              if (i == -1) return;
              final tope = _carrito[i].precioOriginal;
              setState(() => _carrito[i] = _carrito[i].copyWith(precioFinal: precio > tope ? tope : precio));
            },
            metodo: _metodo,
            onCambiarMetodo: (m) => setState(() => _metodo = m),
            ventaACredito: _ventaACredito,
            onCambiarVentaACredito: (v) => setState(() {
              _ventaACredito = v;
              if (!v) _saldoCtrl.clear();
            }),
            saldoCtrl: _saldoCtrl,
            onCambiarSaldo: () => setState(() {}),
            totalVenta: _totalVenta,
            montoPagado: _montoPagado,
            comprobante: _comprobante,
            onElegirComprobante: _elegirComprobante,
            onQuitarComprobante: () => setState(() => _comprobante = null),
            modalidad: _modalidad,
            onCambiarModalidad: (m) => setState(() => _modalidad = m),
            direccionCtrl: _direccionCtrl,
            ciudadCtrl: _ciudadCtrl,
            transportadoraCtrl: _transportadoraCtrl,
            guiaCtrl: _guiaCtrl,
            errorEnvio: _errorEnvio,
            enviando: _enviando,
            onRegistrar: _registrarVenta,
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
            Text(mensaje ?? 'No se pudo cargar.', textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textoPrincipal)),
            const SizedBox(height: 14),
            OutlinedButton(onPressed: onReintentar, child: const Text('Reintentar')),
          ],
        ),
      ),
    );
  }
}

class _Formulario extends StatelessWidget {
  const _Formulario({
    required this.clienteSeleccionado,
    required this.nombreClienteCtrl,
    required this.telefonoClienteCtrl,
    required this.onElegirCliente,
    required this.onQuitarCliente,
    required this.carrito,
    required this.formatoMoneda,
    required this.onAgregarProducto,
    required this.onQuitarProducto,
    required this.onCambiarCantidad,
    required this.onCambiarPrecio,
    required this.metodo,
    required this.onCambiarMetodo,
    required this.ventaACredito,
    required this.onCambiarVentaACredito,
    required this.saldoCtrl,
    required this.onCambiarSaldo,
    required this.totalVenta,
    required this.montoPagado,
    required this.comprobante,
    required this.onElegirComprobante,
    required this.onQuitarComprobante,
    required this.modalidad,
    required this.onCambiarModalidad,
    required this.direccionCtrl,
    required this.ciudadCtrl,
    required this.transportadoraCtrl,
    required this.guiaCtrl,
    required this.errorEnvio,
    required this.enviando,
    required this.onRegistrar,
  });

  final Cliente? clienteSeleccionado;
  final TextEditingController nombreClienteCtrl;
  final TextEditingController telefonoClienteCtrl;
  final VoidCallback onElegirCliente;
  final VoidCallback onQuitarCliente;

  final List<ItemCarrito> carrito;
  final NumberFormat formatoMoneda;
  final VoidCallback onAgregarProducto;
  final void Function(int idProducto) onQuitarProducto;
  final void Function(int idProducto, int cantidad) onCambiarCantidad;
  final void Function(int idProducto, double precio) onCambiarPrecio;

  final MetodoPago metodo;
  final ValueChanged<MetodoPago> onCambiarMetodo;
  final bool ventaACredito;
  final ValueChanged<bool> onCambiarVentaACredito;
  final TextEditingController saldoCtrl;
  final VoidCallback onCambiarSaldo;
  final double totalVenta;
  final double montoPagado;
  final File? comprobante;
  final void Function(ImageSource origen) onElegirComprobante;
  final VoidCallback onQuitarComprobante;

  final ModalidadEntrega modalidad;
  final ValueChanged<ModalidadEntrega> onCambiarModalidad;
  final TextEditingController direccionCtrl;
  final TextEditingController ciudadCtrl;
  final TextEditingController transportadoraCtrl;
  final TextEditingController guiaCtrl;

  final String? errorEnvio;
  final bool enviando;
  final VoidCallback onRegistrar;

  @override
  Widget build(BuildContext context) {
    final saldoPendiente = ventaACredito ? (totalVenta - montoPagado).clamp(0, totalVenta) : 0.0;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
      children: <Widget>[
        if (errorEnvio != null) ...<Widget>[
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.errorFondo, borderRadius: BorderRadius.circular(12)),
            child: Row(
              children: <Widget>[
                const Icon(Icons.error_outline, color: AppColors.error, size: 20),
                const SizedBox(width: 8),
                Expanded(child: Text(errorEnvio!, style: const TextStyle(color: AppColors.error))),
              ],
            ),
          ),
          const SizedBox(height: 14),
        ],

        _SeccionNumerada(numero: '1', titulo: 'CLIENTE', icono: Icons.person_outline),
        const SizedBox(height: 8),
        _Tarjeta(
          child: clienteSeleccionado != null
              ? Row(
                  children: <Widget>[
                    const Icon(Icons.check_circle, color: AppColors.verdeOscuro, size: 18),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(clienteSeleccionado!.nombreCompleto, style: const TextStyle(fontWeight: FontWeight.w700)),
                    ),
                    TextButton(onPressed: onQuitarCliente, child: const Text('Quitar')),
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    OutlinedButton.icon(
                      onPressed: onElegirCliente,
                      icon: const Icon(Icons.search_rounded, size: 20),
                      label: const Text('Elegir cliente existente'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: nombreClienteCtrl,
                      textCapitalization: TextCapitalization.words,
                      decoration: const InputDecoration(labelText: 'O nombre del cliente nuevo *'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: telefonoClienteCtrl,
                      keyboardType: TextInputType.phone,
                      decoration: const InputDecoration(labelText: 'Teléfono (opcional)'),
                    ),
                  ],
                ),
        ),

        const SizedBox(height: 18),
        _SeccionNumerada(numero: '2', titulo: 'PRODUCTOS', icono: Icons.bed_outlined),
        const SizedBox(height: 8),
        _Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              FilledButton.icon(
                onPressed: onAgregarProducto,
                icon: const Icon(Icons.add_rounded),
                label: const Text('Agregar producto'),
              ),
              if (carrito.isEmpty) ...<Widget>[
                const SizedBox(height: 12),
                const Text('Ningún producto agregado todavía.', style: TextStyle(color: AppColors.textoSecundario)),
              ] else ...<Widget>[
                const SizedBox(height: 12),
                ...carrito.map((item) => _FilaCarrito(
                      item: item,
                      formatoMoneda: formatoMoneda,
                      onQuitar: () => onQuitarProducto(item.idProducto),
                      onCambiarCantidad: (c) => onCambiarCantidad(item.idProducto, c),
                      onCambiarPrecio: (p) => onCambiarPrecio(item.idProducto, p),
                    )),
                const Divider(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    const Text('TOTAL', style: TextStyle(fontWeight: FontWeight.w800)),
                    Text(
                      formatoMoneda.format(totalVenta),
                      style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16, color: AppColors.verdeOscuro),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),

        const SizedBox(height: 18),
        _SeccionNumerada(numero: '3', titulo: 'PAGO', icono: Icons.payments_outlined),
        const SizedBox(height: 8),
        _Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const Text('Método de pago', style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: MetodoPago.values.map((m) {
                  final sel = metodo == m;
                  return ChoiceChip(
                    label: Text(m.etiqueta),
                    selected: sel,
                    selectedColor: AppColors.verdeOscuro,
                    labelStyle: TextStyle(color: sel ? Colors.white : AppColors.textoPrincipal, fontWeight: FontWeight.w600),
                    onSelected: (_) => onCambiarMetodo(m),
                  );
                }).toList(),
              ),
              const SizedBox(height: 14),
              SwitchListTile.adaptive(
                contentPadding: EdgeInsets.zero,
                value: ventaACredito,
                onChanged: onCambiarVentaACredito,
                activeThumbColor: AppColors.verdeOscuro,
                title: const Text('Vender a crédito (dejar saldo pendiente)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
              ),
              if (ventaACredito) ...<Widget>[
                TextField(
                  controller: saldoCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Saldo pendiente (Bs.)'),
                  onChanged: (_) => onCambiarSaldo(),
                ),
                const SizedBox(height: 8),
              ],
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                decoration: BoxDecoration(color: AppColors.fondo, borderRadius: BorderRadius.circular(10)),
                child: Column(
                  children: <Widget>[
                    _FilaResumen(etiqueta: 'Monto a cobrar hoy', valor: formatoMoneda.format(montoPagado)),
                    if (ventaACredito)
                      _FilaResumen(
                        etiqueta: 'Saldo pendiente',
                        valor: formatoMoneda.format(saldoPendiente),
                        color: saldoPendiente > 0 ? const Color(0xFFD97706) : AppColors.verdeOscuro,
                      ),
                  ],
                ),
              ),
              if (metodo != MetodoPago.efectivo) ...<Widget>[
                const SizedBox(height: 14),
                Text(
                  comprobante != null ? 'Comprobante adjuntado' : 'Foto del comprobante (opcional)',
                  style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 8),
                Row(
                  children: <Widget>[
                    OutlinedButton.icon(
                      onPressed: () => onElegirComprobante(ImageSource.camera),
                      icon: const Icon(Icons.photo_camera_outlined, size: 18),
                      label: const Text('Cámara'),
                    ),
                    const SizedBox(width: 8),
                    OutlinedButton.icon(
                      onPressed: () => onElegirComprobante(ImageSource.gallery),
                      icon: const Icon(Icons.photo_library_outlined, size: 18),
                      label: const Text('Galería'),
                    ),
                    if (comprobante != null) ...<Widget>[
                      const Spacer(),
                      IconButton(
                        onPressed: onQuitarComprobante,
                        icon: const Icon(Icons.close_rounded, color: AppColors.error),
                      ),
                    ],
                  ],
                ),
                if (comprobante != null) ...<Widget>[
                  const SizedBox(height: 8),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Image.file(comprobante!, height: 120, fit: BoxFit.cover),
                  ),
                ],
              ],
            ],
          ),
        ),

        const SizedBox(height: 18),
        _SeccionNumerada(numero: '4', titulo: 'ENTREGA', icono: Icons.local_shipping_outlined),
        const SizedBox(height: 8),
        _Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: <(ModalidadEntrega, String)>[
                  (ModalidadEntrega.retiro, 'En tienda'),
                  (ModalidadEntrega.domicilio, 'A domicilio'),
                  (ModalidadEntrega.transportadora, 'Transportadora'),
                ].map((par) {
                  final sel = modalidad == par.$1;
                  return ChoiceChip(
                    label: Text(par.$2),
                    selected: sel,
                    selectedColor: AppColors.verdeOscuro,
                    labelStyle: TextStyle(color: sel ? Colors.white : AppColors.textoPrincipal, fontWeight: FontWeight.w600),
                    onSelected: (_) => onCambiarModalidad(par.$1),
                  );
                }).toList(),
              ),
              if (modalidad != ModalidadEntrega.retiro) ...<Widget>[
                const SizedBox(height: 12),
                TextField(controller: direccionCtrl, decoration: const InputDecoration(labelText: 'Dirección *')),
                const SizedBox(height: 12),
                TextField(controller: ciudadCtrl, decoration: const InputDecoration(labelText: 'Ciudad *')),
              ],
              if (modalidad == ModalidadEntrega.transportadora) ...<Widget>[
                const SizedBox(height: 12),
                TextField(controller: transportadoraCtrl, decoration: const InputDecoration(labelText: 'Transportadora *')),
                const SizedBox(height: 12),
                TextField(controller: guiaCtrl, decoration: const InputDecoration(labelText: 'Guía de remisión *')),
              ],
            ],
          ),
        ),

        const SizedBox(height: 24),
        FilledButton.icon(
          onPressed: enviando ? null : onRegistrar,
          icon: enviando
              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2.2, color: Colors.white))
              : const Icon(Icons.save_rounded),
          label: Text(enviando ? 'Registrando...' : 'Registrar venta'),
        ),
      ],
    );
  }
}

class _SeccionNumerada extends StatelessWidget {
  const _SeccionNumerada({required this.numero, required this.titulo, required this.icono});

  final String numero;
  final String titulo;
  final IconData icono;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Icon(icono, size: 16, color: AppColors.verdeSuave),
        const SizedBox(width: 6),
        Text(
          '$numero. $titulo',
          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: AppColors.verdeOscuro, letterSpacing: 0.6),
        ),
      ],
    );
  }
}

class _Tarjeta extends StatelessWidget {
  const _Tarjeta({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.1)),
        boxShadow: <BoxShadow>[
          BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 3)),
        ],
      ),
      child: child,
    );
  }
}

class _FilaResumen extends StatelessWidget {
  const _FilaResumen({required this.etiqueta, required this.valor, this.color});

  final String etiqueta;
  final String valor;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Text(etiqueta, style: const TextStyle(fontSize: 12.5, color: AppColors.textoSecundario)),
          Text(valor, style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: color ?? AppColors.textoPrincipal)),
        ],
      ),
    );
  }
}

class _FilaCarrito extends StatelessWidget {
  const _FilaCarrito({
    required this.item,
    required this.formatoMoneda,
    required this.onQuitar,
    required this.onCambiarCantidad,
    required this.onCambiarPrecio,
  });

  final ItemCarrito item;
  final NumberFormat formatoMoneda;
  final VoidCallback onQuitar;
  final ValueChanged<int> onCambiarCantidad;
  final ValueChanged<double> onCambiarPrecio;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.fondo,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Expanded(
                child: Text(item.nombre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13.5)),
              ),
              IconButton(
                onPressed: onQuitar,
                icon: const Icon(Icons.delete_outline, color: AppColors.error, size: 20),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: <Widget>[
              _BotonPaso(
                icono: Icons.remove,
                onTap: item.cantidad > 1 ? () => onCambiarCantidad(item.cantidad - 1) : null,
              ),
              SizedBox(
                width: 32,
                child: Text('${item.cantidad}', textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w700)),
              ),
              _BotonPaso(
                icono: Icons.add,
                onTap: item.cantidad < item.stockDisponible ? () => onCambiarCantidad(item.cantidad + 1) : null,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  initialValue: item.precioFinal.toStringAsFixed(2),
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  style: const TextStyle(fontSize: 13),
                  decoration: const InputDecoration(isDense: true, labelText: 'Precio'),
                  onFieldSubmitted: (valor) {
                    final precio = double.tryParse(valor.replaceAll(',', '.'));
                    if (precio != null && precio >= 0) onCambiarPrecio(precio);
                  },
                ),
              ),
              const SizedBox(width: 10),
              Text(formatoMoneda.format(item.subtotal), style: const TextStyle(fontWeight: FontWeight.w800, color: AppColors.verdeOscuro)),
            ],
          ),
          if (item.descuentoPorcentaje > 0) ...<Widget>[
            const SizedBox(height: 2),
            Text(
              '-${item.descuentoPorcentaje.toStringAsFixed(1)}% sobre Bs. ${item.precioOriginal.toStringAsFixed(2)}',
              style: const TextStyle(fontSize: 11, color: Color(0xFFD97706), fontWeight: FontWeight.w700),
            ),
          ],
        ],
      ),
    );
  }
}

class _BotonPaso extends StatelessWidget {
  const _BotonPaso({required this.icono, required this.onTap});

  final IconData icono;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        width: 28,
        height: 28,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: onTap != null ? Colors.white : Colors.grey.shade200,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey.shade300),
        ),
        child: Icon(icono, size: 16, color: onTap != null ? AppColors.verdeOscuro : Colors.grey),
      ),
    );
  }
}

/// Selector de producto: mismo patrón que el catálogo (buscador por nombre
/// sobre la lista ya cargada), en bottom sheet como en Módulo 3.
class _SelectorProductoSheet extends StatefulWidget {
  const _SelectorProductoSheet({required this.catalogo});

  final List<ProductoCatalogo> catalogo;

  @override
  State<_SelectorProductoSheet> createState() => _SelectorProductoSheetState();
}

class _SelectorProductoSheetState extends State<_SelectorProductoSheet> {
  String _busqueda = '';
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);

  List<ProductoCatalogo> get _filtrados {
    final q = _busqueda.trim().toLowerCase();
    final disponibles = widget.catalogo.where((p) => !p.agotado);
    if (q.isEmpty) return disponibles.toList();
    return disponibles.where((p) => p.nombre.toLowerCase().contains(q)).toList();
  }

  @override
  Widget build(BuildContext context) {
    return _HojaSelector(
      titulo: 'Elegir producto',
      hintBuscador: 'Buscar por nombre...',
      onBuscar: (v) => setState(() => _busqueda = v),
      child: _filtrados.isEmpty
          ? const Center(child: Padding(padding: EdgeInsets.all(20), child: Text('Sin productos disponibles para esta búsqueda.')))
          : ListView.separated(
              itemCount: _filtrados.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final p = _filtrados[index];
                return InkWell(
                  onTap: () => Navigator.of(context).pop(p),
                  borderRadius: BorderRadius.circular(14),
                  child: Container(
                    padding: const EdgeInsets.all(10),
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
                              Text(p.nombre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13.5)),
                              Text('Stock: ${p.cantidadDisponible}', style: const TextStyle(fontSize: 11.5, color: AppColors.textoSecundario)),
                            ],
                          ),
                        ),
                        Text(_formatoMoneda.format(p.precioVenta), style: const TextStyle(color: AppColors.verdeOscuro, fontWeight: FontWeight.w800)),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}

/// Selector de cliente: mismo patrón, sobre la lista ya cargada.
class _SelectorClienteSheet extends StatefulWidget {
  const _SelectorClienteSheet({required this.clientes});

  final List<Cliente> clientes;

  @override
  State<_SelectorClienteSheet> createState() => _SelectorClienteSheetState();
}

class _SelectorClienteSheetState extends State<_SelectorClienteSheet> {
  String _busqueda = '';

  List<Cliente> get _filtrados {
    final q = _busqueda.trim().toLowerCase();
    if (q.isEmpty) return widget.clientes;
    return widget.clientes
        .where((c) => c.nombreCompleto.toLowerCase().contains(q) || (c.telefono ?? '').contains(q))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    return _HojaSelector(
      titulo: 'Elegir cliente',
      hintBuscador: 'Buscar por nombre o teléfono...',
      onBuscar: (v) => setState(() => _busqueda = v),
      child: _filtrados.isEmpty
          ? const Center(child: Padding(padding: EdgeInsets.all(20), child: Text('No encontrado. Registralo como cliente nuevo.')))
          : ListView.separated(
              itemCount: _filtrados.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final c = _filtrados[index];
                return InkWell(
                  onTap: () => Navigator.of(context).pop(c),
                  borderRadius: BorderRadius.circular(14),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(c.nombreCompleto, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13.5)),
                        Text(c.telefono ?? 'Sin teléfono', style: const TextStyle(fontSize: 11.5, color: AppColors.textoSecundario)),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}

/// Contenedor común de los dos selectores: mismo bottom sheet con drag
/// handle, buscador y título que ya usa el catálogo de Módulo 3.
class _HojaSelector extends StatelessWidget {
  const _HojaSelector({
    required this.titulo,
    required this.hintBuscador,
    required this.onBuscar,
    required this.child,
  });

  final String titulo;
  final String hintBuscador;
  final ValueChanged<String> onBuscar;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * 0.8,
      padding: const EdgeInsets.fromLTRB(18, 14, 18, 20),
      decoration: const BoxDecoration(
        color: AppColors.fondo,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(color: Colors.grey.shade300, borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text(titulo, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
              IconButton(icon: const Icon(Icons.close_rounded), onPressed: () => Navigator.of(context).pop()),
            ],
          ),
          const SizedBox(height: 6),
          TextField(
            autofocus: true,
            decoration: InputDecoration(hintText: hintBuscador, prefixIcon: const Icon(Icons.search)),
            onChanged: onBuscar,
          ),
          const SizedBox(height: 10),
          Expanded(child: child),
        ],
      ),
    );
  }
}
