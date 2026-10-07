import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/precarga_datos.dart';
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
  NuevaVentaScreen({super.key, required this.token, this.esAdmin = false});

  final String token;

  final bool esAdmin;

  @override
  State<NuevaVentaScreen> createState() => _NuevaVentaScreenState();
}

class _NuevaVentaScreenState extends State<NuevaVentaScreen> {
  final _clientesRepository = ClientesRepository();
  final _catalogoRepository = CatalogoRepository();
  final _ventasRepository = VentasRepository();
  final _formatoMoneda = NumberFormat.currency(
    locale: 'es_BO',
    symbol: 'Bs. ',
    decimalDigits: 2,
  );

  _EstadoCarga _estado = _EstadoCarga.cargando;
  String? _errorCarga;
  List<Cliente> _clientes = <Cliente>[];
  List<ProductoCatalogo> _catalogo = <ProductoCatalogo>[];

  // Cliente
  Cliente? _clienteSeleccionado;
  final _nombreClienteCtrl = TextEditingController();
  final _telefonoClienteCtrl = TextEditingController();
  final _ciClienteCtrl = TextEditingController();

  // Productos
  final List<ItemCarrito> _carrito = <ItemCarrito>[];

  // Pago
  MetodoPago _metodo = MetodoPago.efectivo;
  bool _ventaACredito = false;
  // Hasta cuándo se espera el pago pendiente (opcional).
  DateTime? _fechaLimite;
  // Si se usó un botón rápido, se manda el plazo en días (el servidor calcula la fecha).
  int? _plazoDias;
  final _saldoCtrl = TextEditingController();
  File? _comprobante;
  final _referenciaCtrl = TextEditingController();

  // Entrega
  ModalidadEntrega _modalidad = ModalidadEntrega.retiro;
  // Domicilio y transportadora arrancan Pendiente; se pasa a Entregado cuando
  // llega. En tienda no se elige.
  EstadoEntrega _estadoEntrega = EstadoEntrega.pendiente;
  final _direccionCtrl = TextEditingController();
  final _ciudadCtrl = TextEditingController();
  final _transportadoraCtrl = TextEditingController();
  final _guiaCtrl = TextEditingController();

  bool _enviando = false;
  String? _errorEnvio;

  // Errores de validación, uno por campo, para mostrarlos junto al campo
  // que los provoca en vez de en un único banner arriba del formulario.
  String? _errorCliente;
  String? _errorProductos;
  String? _errorSaldo;
  String? _errorCiudad;

  final _scrollController = ScrollController();
  final _keyCliente = GlobalKey();
  final _keyProductos = GlobalKey();
  final _keySaldo = GlobalKey();
  final _keyCiudad = GlobalKey();

  @override
  void initState() {
    super.initState();
    // El error "obligatorio" se quita en cuanto se escribe el nombre.
    _nombreClienteCtrl.addListener(() {
      if (_errorCliente != null && _nombreClienteCtrl.text.trim().isNotEmpty) {
        setState(() => _errorCliente = null);
      }
    });
    _cargarDatos();
  }

  @override
  void dispose() {
    _nombreClienteCtrl.dispose();
    _telefonoClienteCtrl.dispose();
    _ciClienteCtrl.dispose();
    _referenciaCtrl.dispose();
    _saldoCtrl.dispose();
    _direccionCtrl.dispose();
    _ciudadCtrl.dispose();
    _transportadoraCtrl.dispose();
    _guiaCtrl.dispose();
    _scrollController.dispose();
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

  double get _totalVenta =>
      _carrito.fold(0.0, (acc, item) => acc + item.subtotal);

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
      setState(
        () => _carrito[indice] = actual.copyWith(cantidad: actual.cantidad + 1),
      );
    } else {
      setState(() {
        _carrito.add(
          ItemCarrito(
            idProducto: producto.id,
            nombre: producto.nombre,
            skuProducto: producto.sku,
            precioOriginal: producto.precioVenta,
            precioFinal: producto.precioVenta,
            cantidad: 1,
            stockDisponible: producto.cantidadDisponible,
          ),
        );
      });
    }
  }

  /// Detalle de un producto que ya está en la venta: solo lectura, con un
  /// botón OK. También se cierra tocando fuera de la hoja.
  void _verDetalleProducto(int idProducto) {
    final producto = _catalogo.where((p) => p.id == idProducto).firstOrNull;
    if (producto == null) return;
    // Ventana flotante en el centro: sin X ni asa; se cierra con OK o
    // tocando fuera.
    showDialog<void>(
      context: context,
      builder: (dialogContext) => Dialog(
        backgroundColor: AppColors.fondo,
        insetPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 40),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: _DetalleProductoVenta(
            producto: producto,
            formatoMoneda: _formatoMoneda,
            onAtras: () => Navigator.of(dialogContext).pop(),
            textoAtras: 'OK',
          ),
        ),
      ),
    );
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

  /// Valida cada campo por separado y deja el mensaje correspondiente en su
  /// propio `_errorX`, para mostrarlo junto al campo que falló. Devuelve el
  /// grupo del primer campo con error (0 cliente, 1 productos, 2 saldo, 3 ciudad),
  /// o null si todo está bien.
  int? _validarCampos() {
    final sinCliente =
        _clienteSeleccionado == null && _nombreClienteCtrl.text.trim().isEmpty;
    _errorCliente = sinCliente ? 'El nombre del cliente es obligatorio' : null;

    _errorProductos = _carrito.isEmpty
        ? 'Debe agregar al menos un producto'
        : _carrito.any((item) => item.cantidad < 1)
        ? 'Hay un producto con cantidad inválida'
        : null;

    if (_ventaACredito) {
      final saldo = double.tryParse(_saldoCtrl.text.replaceAll(',', '.'));
      _errorSaldo = (saldo != null && saldo > _totalVenta)
          ? 'El saldo pendiente no puede superar el total de la venta'
          : null;
    } else {
      _errorSaldo = null;
    }

    // Solo el envío por transportadora exige un dato: la ciudad. La dirección,
    // la transportadora y la guía se pueden completar después.
    final requiereCiudad = _modalidad == ModalidadEntrega.transportadora;
    _errorCiudad = requiereCiudad && _ciudadCtrl.text.trim().isEmpty
        ? 'La ciudad es obligatoria para el envío por transportadora'
        : null;

    if (_errorCliente != null) return 0;
    if (_errorProductos != null) return 1;
    if (_errorSaldo != null) return 2;
    if (_errorCiudad != null) return 3;
    return null;
  }

  Future<void> _registrarVenta() async {
    int? campoConError;
    setState(() => campoConError = _validarCampos());
    if (campoConError != null) {
      final llave = <GlobalKey>[
        _keyCliente,
        _keyProductos,
        _keySaldo,
        _keyCiudad,
      ][campoConError!];
      // Se espera a que termine este frame para que el campo ya tenga su
      // errorText dibujado antes de calcular hasta dónde desplazar la vista.
      WidgetsBinding.instance.addPostFrameCallback((_) {
        final contexto = llave.currentContext;
        if (contexto != null) {
          Scrollable.ensureVisible(
            contexto,
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeInOut,
            alignment: 0.1,
          );
        }
      });
      return;
    }

    setState(() {
      _enviando = true;
      _errorEnvio = null;
    });

    try {
      final request = NuevaVentaRequest(
        idCliente: _clienteSeleccionado?.id,
        nombreClienteInvitado: _clienteSeleccionado == null
            ? _nombreClienteCtrl.text.trim()
            : null,
        telefonoClienteInvitado: _clienteSeleccionado == null
            ? _telefonoClienteCtrl.text.trim()
            : null,
        ciClienteInvitado: _clienteSeleccionado == null
            ? _ciClienteCtrl.text.trim()
            : null,
        referenciaPago: _metodo != MetodoPago.efectivo
            ? _referenciaCtrl.text.trim()
            : null,
        metodoPago: _metodo,
        items: _carrito,
        montoPagado: _montoPagado,
        // Solo cuenta si la venta queda con pago pendiente.
        plazoDiasPago: _ventaACredito && _montoPagado < _totalVenta
            ? _plazoDias
            : null,
        fechaLimitePago:
            _ventaACredito && _montoPagado < _totalVenta && _plazoDias == null
            ? _fechaLimite
            : null,
        modalidadEntrega: _modalidad,
        estadoEntrega: _modalidad != ModalidadEntrega.retiro
            ? _estadoEntrega
            : null,
        direccionDestino: _modalidad != ModalidadEntrega.retiro
            ? _direccionCtrl.text.trim()
            : null,
        ciudad: _modalidad == ModalidadEntrega.transportadora
            ? _ciudadCtrl.text.trim()
            : null,
        transportadora: _modalidad == ModalidadEntrega.transportadora
            ? _transportadoraCtrl.text.trim()
            : null,
        guiaRemision: _modalidad == ModalidadEntrega.transportadora
            ? _guiaCtrl.text.trim()
            : null,
      );

      final creada = await _ventasRepository.crearVenta(request, widget.token);

      // Igual que el formulario web: la venta ya quedó registrada aunque
      // esto falle, así que un error acá no revierte nada, solo avisa.
      if (_comprobante != null && creada.pagos.isNotEmpty) {
        try {
          await _ventasRepository.adjuntarComprobante(
            creada.pagos.first.id,
            _comprobante!,
            widget.token,
          );
        } catch (_) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text(
                  'La venta se registró, pero no se pudo subir el comprobante.',
                ),
              ),
            );
          }
        }
      }

      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      final excedido = error.codigo == 'PRECIO_EXCEDE_CATALOGO'
          ? _carrito.where((i) => i.precioFinal > i.precioOriginal).firstOrNull
          : null;
      setState(() {
        _errorEnvio = excedido == null ? error.mensaje : null;
        _enviando = false;
      });
      if (excedido != null) await _avisarPrecioExcedido(excedido);
    } catch (_) {
      setState(() {
        _errorEnvio = 'No se pudo registrar la venta.';
        _enviando = false;
      });
    }
  }

  /// El servidor rechazó un precio por encima del de catálogo. El ADMIN puede
  /// subir el precio de catálogo y seguir con la venta; el EMPLEADO solo recibe
  /// el aviso. El carrito no se toca en ningún caso.
  Future<void> _avisarPrecioExcedido(ItemCarrito item) async {
    final actualizar = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: Text(
          'El precio supera el de catálogo (${item.precioOriginal.toStringAsFixed(2)} Bs)',
        ),
        content: Text(
          '${item.nombre}: precio escrito Bs. ${item.precioFinal.toStringAsFixed(2)}.'
          '${widget.esAdmin ? '' : '\n\nSolo el administrador puede cambiar el precio de venta.'}',
        ),
        actions: widget.esAdmin
            ? <Widget>[
                TextButton(
                  onPressed: () => Navigator.of(contexto).pop(false),
                  child: const Text('Cancelar'),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(contexto).pop(true),
                  child: const Text('Actualizar precio'),
                ),
              ]
            : <Widget>[
                FilledButton(
                  onPressed: () => Navigator.of(contexto).pop(false),
                  child: const Text('Entendido'),
                ),
              ],
      ),
    );
    if (actualizar != true || !mounted) return;

    setState(() => _enviando = true);
    try {
      await _catalogoRepository.actualizarPrecioVenta(
        item.idProducto,
        item.precioFinal,
        widget.token,
      );
      if (!mounted) return;
      setState(() {
        final i = _carrito.indexWhere((x) => x.idProducto == item.idProducto);
        if (i != -1)
          _carrito[i] = _carrito[i].copyWith(precioOriginal: item.precioFinal);
        _enviando = false;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorEnvio = error.mensaje;
        _enviando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorEnvio = 'No se pudo actualizar el precio del producto.';
        _enviando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Nueva venta'),
        // Se muestra como formulario flotante: se cierra con la X.
        automaticallyImplyLeading: false,
        actions: <Widget>[
          IconButton(
            tooltip: 'Cerrar',
            icon: const Icon(Icons.close_rounded),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
      body: switch (_estado) {
        _EstadoCarga.cargando => Center(
          child: CircularProgressIndicator(color: AppColors.verdeOscuro),
        ),
        _EstadoCarga.error => _CentroError(
          mensaje: _errorCarga,
          onReintentar: _cargarDatos,
        ),
        _EstadoCarga.listo => _Formulario(
          scrollController: _scrollController,
          onCancelar: () => Navigator.of(context).pop(),
          onVerDetalleProducto: _verDetalleProducto,
          fechaLimite: _fechaLimite,
          plazoDias: _plazoDias,
          onCambiarFechaLimite: (f, d) => setState(() {
            _fechaLimite = f;
            _plazoDias = d;
          }),
          ciClienteCtrl: _ciClienteCtrl,
          referenciaCtrl: _referenciaCtrl,
          clienteSeleccionado: _clienteSeleccionado,
          nombreClienteCtrl: _nombreClienteCtrl,
          telefonoClienteCtrl: _telefonoClienteCtrl,
          onElegirCliente: _elegirCliente,
          onQuitarCliente: () => setState(() => _clienteSeleccionado = null),
          errorCliente: _errorCliente,
          keyCliente: _keyCliente,
          carrito: _carrito,
          errorProductos: _errorProductos,
          keyProductos: _keyProductos,
          formatoMoneda: _formatoMoneda,
          onAgregarProducto: _elegirProducto,
          onQuitarProducto: (id) =>
              setState(() => _carrito.removeWhere((i) => i.idProducto == id)),
          onCambiarCantidad: (id, cantidad) {
            final i = _carrito.indexWhere((item) => item.idProducto == id);
            if (i != -1)
              setState(
                () => _carrito[i] = _carrito[i].copyWith(cantidad: cantidad),
              );
          },
          onCambiarPrecio: (id, precio) {
            final i = _carrito.indexWhere((item) => item.idProducto == id);
            if (i == -1) return;
            // Sin tope: si el precio supera el de catálogo, el servidor lo
            // rechaza y se muestra el aviso (_avisarPrecioExcedido).
            setState(
              () => _carrito[i] = _carrito[i].copyWith(precioFinal: precio),
            );
          },
          metodo: _metodo,
          onCambiarMetodo: (m) => setState(() => _metodo = m),
          ventaACredito: _ventaACredito,
          onCambiarVentaACredito: (v) => setState(() {
            _ventaACredito = v;
            if (!v) {
              _saldoCtrl.clear();
              _fechaLimite = null;
              _plazoDias = null;
            }
          }),
          saldoCtrl: _saldoCtrl,
          onCambiarSaldo: () => setState(() {}),
          errorSaldo: _errorSaldo,
          keySaldo: _keySaldo,
          totalVenta: _totalVenta,
          montoPagado: _montoPagado,
          comprobante: _comprobante,
          onElegirComprobante: _elegirComprobante,
          onQuitarComprobante: () => setState(() => _comprobante = null),
          modalidad: _modalidad,
          onCambiarModalidad: (m) => setState(() {
            // Al pasar de "en tienda" a un envío, el estado vuelve a Pendiente.
            if (m != ModalidadEntrega.retiro &&
                _modalidad == ModalidadEntrega.retiro) {
              _estadoEntrega = EstadoEntrega.pendiente;
            }
            _modalidad = m;
          }),
          estadoEntrega: _estadoEntrega,
          onCambiarEstadoEntrega: (e) => setState(() => _estadoEntrega = e),
          esAdmin: widget.esAdmin,
          direccionCtrl: _direccionCtrl,
          ciudadCtrl: _ciudadCtrl,
          transportadoraCtrl: _transportadoraCtrl,
          guiaCtrl: _guiaCtrl,
          errorCiudad: _errorCiudad,
          keyCiudad: _keyCiudad,
          errorEnvio: _errorEnvio,
          enviando: _enviando,
          onRegistrar: _registrarVenta,
        ),
      },
    );
  }
}

class _CentroError extends StatelessWidget {
  _CentroError({required this.mensaje, required this.onReintentar});

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
            Icon(Icons.cloud_off_rounded, color: AppColors.error, size: 40),
            const SizedBox(height: 10),
            Text(
              mensaje ?? 'No se pudo cargar.',
              textAlign: TextAlign.center,
              style: TextStyle(color: AppColors.textoPrincipal),
            ),
            const SizedBox(height: 14),
            OutlinedButton(
              onPressed: onReintentar,
              child: const Text('Reintentar'),
            ),
          ],
        ),
      ),
    );
  }
}

class _Formulario extends StatelessWidget {
  _Formulario({
    required this.scrollController,
    required this.onCancelar,
    required this.onVerDetalleProducto,
    required this.fechaLimite,
    required this.plazoDias,
    required this.onCambiarFechaLimite,
    required this.ciClienteCtrl,
    required this.referenciaCtrl,
    required this.clienteSeleccionado,
    required this.nombreClienteCtrl,
    required this.telefonoClienteCtrl,
    required this.onElegirCliente,
    required this.onQuitarCliente,
    required this.errorCliente,
    required this.keyCliente,
    required this.carrito,
    required this.formatoMoneda,
    required this.onAgregarProducto,
    required this.onQuitarProducto,
    required this.onCambiarCantidad,
    required this.onCambiarPrecio,
    required this.errorProductos,
    required this.keyProductos,
    required this.metodo,
    required this.onCambiarMetodo,
    required this.ventaACredito,
    required this.onCambiarVentaACredito,
    required this.saldoCtrl,
    required this.onCambiarSaldo,
    required this.errorSaldo,
    required this.keySaldo,
    required this.totalVenta,
    required this.montoPagado,
    required this.comprobante,
    required this.onElegirComprobante,
    required this.onQuitarComprobante,
    required this.modalidad,
    required this.onCambiarModalidad,
    required this.estadoEntrega,
    required this.onCambiarEstadoEntrega,
    required this.esAdmin,
    required this.direccionCtrl,
    required this.ciudadCtrl,
    required this.transportadoraCtrl,
    required this.guiaCtrl,
    required this.errorCiudad,
    required this.keyCiudad,
    required this.errorEnvio,
    required this.enviando,
    required this.onRegistrar,
  });

  final ScrollController scrollController;

  final VoidCallback onCancelar;
  final void Function(int idProducto) onVerDetalleProducto;
  final DateTime? fechaLimite;
  final int? plazoDias;
  final void Function(DateTime? fecha, int? dias) onCambiarFechaLimite;
  final TextEditingController ciClienteCtrl;
  final TextEditingController referenciaCtrl;

  final Cliente? clienteSeleccionado;
  final TextEditingController nombreClienteCtrl;
  final TextEditingController telefonoClienteCtrl;
  final VoidCallback onElegirCliente;
  final VoidCallback onQuitarCliente;
  final String? errorCliente;
  final GlobalKey keyCliente;

  final List<ItemCarrito> carrito;
  final NumberFormat formatoMoneda;
  final VoidCallback onAgregarProducto;
  final void Function(int idProducto) onQuitarProducto;
  final void Function(int idProducto, int cantidad) onCambiarCantidad;
  final void Function(int idProducto, double precio) onCambiarPrecio;
  final String? errorProductos;
  final GlobalKey keyProductos;

  final MetodoPago metodo;
  final ValueChanged<MetodoPago> onCambiarMetodo;
  final bool ventaACredito;
  final ValueChanged<bool> onCambiarVentaACredito;
  final TextEditingController saldoCtrl;
  final VoidCallback onCambiarSaldo;
  final String? errorSaldo;
  final GlobalKey keySaldo;
  final double totalVenta;
  final double montoPagado;
  final File? comprobante;
  final void Function(ImageSource origen) onElegirComprobante;
  final VoidCallback onQuitarComprobante;

  final ModalidadEntrega modalidad;
  final ValueChanged<ModalidadEntrega> onCambiarModalidad;
  final EstadoEntrega estadoEntrega;
  final ValueChanged<EstadoEntrega> onCambiarEstadoEntrega;

  final bool esAdmin;
  final TextEditingController direccionCtrl;
  final TextEditingController ciudadCtrl;
  final TextEditingController transportadoraCtrl;
  final TextEditingController guiaCtrl;
  final String? errorCiudad;
  final GlobalKey keyCiudad;

  final String? errorEnvio;
  final bool enviando;
  final VoidCallback onRegistrar;

  @override
  Widget build(BuildContext context) {
    final saldoPendiente = ventaACredito
        ? (totalVenta - montoPagado).clamp(0, totalVenta)
        : 0.0;
    const espacio = SizedBox(height: 12);

    // Un solo formulario: cada grupo es una tarjeta sin título, y cada campo
    // lleva su ícono. Solo el contenido se desplaza; abajo quedan fijos el
    // total y los botones Cancelar / Registrar.
    return Column(
      children: <Widget>[
        Expanded(
          child: ListView(
            controller: scrollController,
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            children: <Widget>[
              if (errorEnvio != null) ...<Widget>[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.errorFondo,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: <Widget>[
                      Icon(
                        Icons.error_outline,
                        color: AppColors.error,
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          errorEnvio!,
                          style: TextStyle(color: AppColors.error),
                        ),
                      ),
                    ],
                  ),
                ),
                espacio,
              ],

              // Cliente
              _Tarjeta(
                child: clienteSeleccionado != null
                    ? Row(
                        children: <Widget>[
                          Icon(Icons.person, color: AppColors.verdeOscuro),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Text(
                                  clienteSeleccionado!.nombreCompleto,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                // Celular y CI a la vista, sin tocar nada más.
                                if (_datosCliente(clienteSeleccionado!) != null)
                                  Text(
                                    _datosCliente(clienteSeleccionado!)!,
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      color: AppColors.textoSecundario,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          IconButton(
                            tooltip: 'Quitar cliente',
                            onPressed: onQuitarCliente,
                            icon: const Icon(Icons.close_rounded),
                          ),
                        ],
                      )
                    : Column(
                        children: <Widget>[
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Expanded(
                                child: TextField(
                                  key: keyCliente,
                                  controller: nombreClienteCtrl,
                                  textCapitalization: TextCapitalization.words,
                                  decoration: InputDecoration(
                                    labelText: 'Cliente *',
                                    prefixIcon: const Icon(
                                      Icons.person_outline,
                                    ),
                                    errorText: errorCliente,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Padding(
                                padding: const EdgeInsets.only(top: 4),
                                child: IconButton.filledTonal(
                                  tooltip: 'Elegir cliente existente',
                                  onPressed: onElegirCliente,
                                  icon: const Icon(Icons.search_rounded),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Row(
                            children: <Widget>[
                              Expanded(
                                child: TextField(
                                  controller: telefonoClienteCtrl,
                                  keyboardType: TextInputType.phone,
                                  decoration: const InputDecoration(
                                    labelText: 'Teléfono',
                                    prefixIcon: Icon(Icons.phone_outlined),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: TextField(
                                  controller: ciClienteCtrl,
                                  decoration: const InputDecoration(
                                    labelText: 'CI',
                                    prefixIcon: Icon(Icons.badge_outlined),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
              ),
              espacio,

              // Productos
              _Tarjeta(
                key: keyProductos,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    FilledButton.icon(
                      onPressed: onAgregarProducto,
                      icon: const Icon(Icons.add_shopping_cart_rounded),
                      label: const Text('Agregar producto'),
                    ),
                    if (errorProductos != null) ...<Widget>[
                      const SizedBox(height: 6),
                      Text(
                        errorProductos!,
                        style: TextStyle(
                          color: AppColors.error,
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                    if (carrito.isNotEmpty) ...<Widget>[
                      const SizedBox(height: 10),
                      ...carrito.map(
                        (item) => _FilaCarrito(
                          item: item,
                          formatoMoneda: formatoMoneda,
                          onQuitar: () => onQuitarProducto(item.idProducto),
                          onCambiarCantidad: (c) =>
                              onCambiarCantidad(item.idProducto, c),
                          onCambiarPrecio: (p) =>
                              onCambiarPrecio(item.idProducto, p),
                          onVerDetalle: () =>
                              onVerDetalleProducto(item.idProducto),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              espacio,

              // Pago
              _Tarjeta(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        for (final (m, icono) in <(MetodoPago, IconData)>[
                          (MetodoPago.efectivo, Icons.payments_outlined),
                          (
                            MetodoPago.transferencia,
                            Icons.account_balance_outlined,
                          ),
                          (MetodoPago.qr, Icons.qr_code_2_rounded),
                        ])
                          Expanded(
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 3,
                              ),
                              child: _OpcionIcono(
                                icono: icono,
                                etiqueta: m.etiqueta,
                                seleccionado: metodo == m,
                                onTap: () => onCambiarMetodo(m),
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: <Widget>[
                        Icon(
                          Icons.schedule_rounded,
                          color: AppColors.textoSecundario,
                          size: 22,
                        ),
                        const SizedBox(width: 10),
                        const Expanded(
                          child: Text(
                            '¿Con pago pendiente?',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        Switch.adaptive(
                          value: ventaACredito,
                          onChanged: onCambiarVentaACredito,
                          activeThumbColor: AppColors.verdeOscuro,
                        ),
                      ],
                    ),
                    if (ventaACredito) ...<Widget>[
                      TextField(
                        key: keySaldo,
                        controller: saldoCtrl,
                        // Al activar el interruptor el campo ya está listo para
                        // escribir (con el cursor adentro).
                        autofocus: true,
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        decoration: InputDecoration(
                          labelText: 'Pago pendiente (Bs.)',
                          floatingLabelBehavior: FloatingLabelBehavior.always,
                          filled: true,
                          fillColor: AppColors.tarjeta,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: const BorderSide(
                              color: Color(0xFF3E9B94),
                            ),
                          ),
                          enabledBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: const BorderSide(
                              color: Color(0xFF3E9B94),
                            ),
                          ),
                          focusedBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: const BorderSide(
                              color: Color(0xFF3E9B94),
                              width: 2,
                            ),
                          ),
                          prefixIcon: const Icon(
                            Icons.hourglass_bottom_rounded,
                          ),
                          errorText: errorSaldo,
                        ),
                        onChanged: (_) => onCambiarSaldo(),
                      ),
                      const SizedBox(height: 10),
                      _SelectorFechaLimite(
                        fecha: fechaLimite,
                        plazoDias: plazoDias,
                        onCambiar: onCambiarFechaLimite,
                      ),
                      const SizedBox(height: 8),
                    ],
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.fondo,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Column(
                        children: <Widget>[
                          _FilaResumen(
                            etiqueta: 'Total venta',
                            valor: formatoMoneda.format(totalVenta),
                          ),
                          _FilaResumen(
                            etiqueta: 'Cobro hoy',
                            valor: formatoMoneda.format(montoPagado),
                          ),
                          if (ventaACredito)
                            _FilaResumen(
                              etiqueta: 'Pago pendiente',
                              valor: formatoMoneda.format(saldoPendiente),
                              color: saldoPendiente > 0
                                  ? AppColors.acAmbar
                                  : AppColors.verdeOscuro,
                            ),
                        ],
                      ),
                    ),
                    if (metodo != MetodoPago.efectivo) ...<Widget>[
                      const SizedBox(height: 10),
                      TextField(
                        controller: referenciaCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Referencia del pago',
                          prefixIcon: Icon(Icons.tag_rounded),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: <Widget>[
                          OutlinedButton.icon(
                            onPressed: () =>
                                onElegirComprobante(ImageSource.camera),
                            icon: const Icon(
                              Icons.photo_camera_outlined,
                              size: 18,
                            ),
                            label: const Text('Cámara'),
                          ),
                          const SizedBox(width: 8),
                          OutlinedButton.icon(
                            onPressed: () =>
                                onElegirComprobante(ImageSource.gallery),
                            icon: const Icon(
                              Icons.photo_library_outlined,
                              size: 18,
                            ),
                            label: const Text('Galería'),
                          ),
                          if (comprobante != null) ...<Widget>[
                            const Spacer(),
                            IconButton(
                              tooltip: 'Quitar comprobante',
                              onPressed: onQuitarComprobante,
                              icon: Icon(
                                Icons.close_rounded,
                                color: AppColors.error,
                              ),
                            ),
                          ],
                        ],
                      ),
                      if (comprobante != null) ...<Widget>[
                        const SizedBox(height: 8),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(12),
                          child: Image.file(
                            comprobante!,
                            height: 90,
                            width: double.infinity,
                            fit: BoxFit.cover,
                          ),
                        ),
                      ],
                    ],
                  ],
                ),
              ),
              espacio,

              // Entrega
              _Tarjeta(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        for (final (m, icono, texto)
                            in <(ModalidadEntrega, IconData, String)>[
                              (
                                ModalidadEntrega.retiro,
                                Icons.storefront_outlined,
                                'En tienda',
                              ),
                              (
                                ModalidadEntrega.domicilio,
                                Icons.home_outlined,
                                'Domicilio',
                              ),
                              (
                                ModalidadEntrega.transportadora,
                                Icons.local_shipping_outlined,
                                'Transporte',
                              ),
                            ])
                          Expanded(
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 3,
                              ),
                              child: _OpcionIcono(
                                icono: icono,
                                etiqueta: texto,
                                seleccionado: modalidad == m,
                                onTap: () => onCambiarModalidad(m),
                              ),
                            ),
                          ),
                      ],
                    ),
                    // En tienda: sin campos, la venta queda entregada.
                    if (modalidad == ModalidadEntrega.retiro) ...<Widget>[
                      const SizedBox(height: 8),
                      Text(
                        'La venta queda entregada al registrarla.',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.textoSecundario,
                        ),
                      ),
                    ],
                    // Estado de la entrega: en domicilio y transportadora.
                    if (modalidad != ModalidadEntrega.retiro) ...<Widget>[
                      const SizedBox(height: 10),
                      Row(
                        children: <Widget>[
                          for (final (e, icono) in <(EstadoEntrega, IconData)>[
                            (
                              EstadoEntrega.entregado,
                              Icons.check_circle_rounded,
                            ),
                            (EstadoEntrega.pendiente, Icons.schedule_rounded),
                          ])
                            Expanded(
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 3,
                                ),
                                child: _OpcionIcono(
                                  icono: icono,
                                  etiqueta: e.etiqueta,
                                  colorOpcion: const Color(0xFF16A34A),
                                  seleccionado: estadoEntrega == e,
                                  onTap: () => onCambiarEstadoEntrega(e),
                                ),
                              ),
                            ),
                        ],
                      ),
                    ],
                    // Domicilio: solo la dirección, opcional.
                    if (modalidad == ModalidadEntrega.domicilio) ...<Widget>[
                      const SizedBox(height: 10),
                      TextField(
                        controller: direccionCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Dirección',
                          prefixIcon: Icon(Icons.location_on_outlined),
                        ),
                      ),
                    ],
                    // Transportadora: la ciudad es obligatoria; lo demás se
                    // puede completar después desde el detalle de la venta.
                    if (modalidad ==
                        ModalidadEntrega.transportadora) ...<Widget>[
                      const SizedBox(height: 10),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Expanded(
                            child: TextField(
                              key: keyCiudad,
                              controller: ciudadCtrl,
                              decoration: InputDecoration(
                                labelText: 'Ciudad *',
                                prefixIcon: const Icon(
                                  Icons.location_city_outlined,
                                ),
                                errorText: errorCiudad,
                                errorMaxLines: 2,
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextField(
                              controller: transportadoraCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Transportadora',
                                prefixIcon: Icon(Icons.local_shipping_outlined),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: <Widget>[
                          Expanded(
                            child: TextField(
                              controller: direccionCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Dirección',
                                prefixIcon: Icon(Icons.location_on_outlined),
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextField(
                              controller: guiaCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Guía',
                                prefixIcon: Icon(
                                  Icons.confirmation_number_outlined,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
        _BarraAcciones(
          total: carrito.isEmpty ? null : formatoMoneda.format(montoPagado),
          enviando: enviando,
          onCancelar: onCancelar,
          onRegistrar: onRegistrar,
        ),
      ],
    );
  }
}

/// Opción con el ícono y el texto en una sola fila (método de pago, entrega,
/// estado): las tres caben lado a lado y ocupan poca altura.
class _OpcionIcono extends StatelessWidget {
  _OpcionIcono({
    required this.icono,
    required this.etiqueta,
    required this.seleccionado,
    required this.onTap,
    this.colorOpcion,
  });

  final IconData icono;
  final String etiqueta;
  final bool seleccionado;
  final VoidCallback onTap;

  /// Color de fondo cuando está elegida (el estado de entrega usa uno por opción).
  final Color? colorOpcion;

  @override
  Widget build(BuildContext context) {
    final colorSeleccion = colorOpcion ?? AppColors.verdeOscuro;
    final frente = seleccionado ? Colors.white : AppColors.textoPrincipal;
    return Material(
      color: seleccionado ? colorSeleccion : AppColors.fondo,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          height: 46,
          padding: const EdgeInsets.symmetric(horizontal: 6),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: seleccionado
                  ? colorSeleccion
                  : AppColors.verdeOscuro.withValues(alpha: 0.18),
              width: seleccionado ? 1.5 : 1,
            ),
          ),
          child: Center(
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Icon(
                    icono,
                    size: 19,
                    color: seleccionado ? Colors.white : colorSeleccion,
                  ),
                  const SizedBox(width: 5),
                  Text(
                    etiqueta,
                    maxLines: 1,
                    style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                      color: frente,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Fecha límite del pago pendiente: tres botones rápidos (7, 15 y 30 días) y
/// un calendario. Los botones mandan los DÍAS (el servidor calcula la fecha con
/// su reloj); el calendario manda la fecha exacta, que el servidor valida.
class _SelectorFechaLimite extends StatelessWidget {
  _SelectorFechaLimite({
    required this.fecha,
    required this.plazoDias,
    required this.onCambiar,
  });

  final DateTime? fecha;
  final int? plazoDias;
  final void Function(DateTime? fecha, int? dias) onCambiar;

  /// Solo para mostrar "aprox."; la fecha real la calcula el servidor.
  static DateTime _enDias(int dias) {
    final hoy = DateTime.now();
    return DateTime(hoy.year, hoy.month, hoy.day).add(Duration(days: dias));
  }

  Future<void> _elegirEnCalendario(BuildContext context) async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: plazoDias == null
          ? (fecha ?? _enDias(7))
          : _enDias(plazoDias!),
      firstDate: DateTime(hoy.year, hoy.month, hoy.day),
      lastDate: DateTime(hoy.year + 2, hoy.month, hoy.day),
    );
    if (elegida != null) onCambiar(elegida, null);
  }

  @override
  Widget build(BuildContext context) {
    final formato = DateFormat('dd/MM/yyyy');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Row(
          children: <Widget>[
            Icon(
              Icons.event_outlined,
              size: 20,
              color: AppColors.textoSecundario,
            ),
            SizedBox(width: 8),
            Text(
              'Fecha límite de pago',
              style: TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: <Widget>[
            for (final dias in <int>[7, 15, 30])
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 3),
                  child: _OpcionIcono(
                    icono: Icons.today_outlined,
                    etiqueta: '$dias días',
                    seleccionado: plazoDias == dias,
                    onTap: () => onCambiar(_enDias(dias), dias),
                  ),
                ),
              ),
            const SizedBox(width: 3),
            IconButton.filledTonal(
              tooltip: 'Elegir en el calendario',
              onPressed: () => _elegirEnCalendario(context),
              icon: const Icon(Icons.calendar_month_outlined),
            ),
          ],
        ),
        if (fecha != null || plazoDias != null) ...<Widget>[
          const SizedBox(height: 6),
          Row(
            children: <Widget>[
              Expanded(
                child: Text(
                  plazoDias != null
                      ? 'Dentro de $plazoDias días'
                      : 'Vence el ${formato.format(fecha!)}',
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: AppColors.verdeOscuro,
                  ),
                ),
              ),
              TextButton(
                onPressed: () => onCambiar(null, null),
                child: const Text('Quitar'),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

/// Parte fija de abajo: el total y los botones Cancelar / Registrar venta.
class _BarraAcciones extends StatelessWidget {
  _BarraAcciones({
    required this.total,
    required this.enviando,
    required this.onCancelar,
    required this.onRegistrar,
  });

  final String? total;
  final bool enviando;
  final VoidCallback onCancelar;
  final VoidCallback onRegistrar;

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 8,
      color: Theme.of(context).scaffoldBackgroundColor,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 10),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              if (total != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: <Widget>[
                      const Text(
                        'Cobro hoy',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
                      Text(
                        total!,
                        style: TextStyle(
                          fontWeight: FontWeight.w900,
                          fontSize: 18,
                          color: AppColors.verdeOscuro,
                        ),
                      ),
                    ],
                  ),
                ),
              Row(
                children: <Widget>[
                  Expanded(
                    flex: 2,
                    child: OutlinedButton(
                      onPressed: enviando ? null : onCancelar,
                      child: const Text('Cancelar'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    flex: 3,
                    child: FilledButton.icon(
                      onPressed: enviando ? null : onRegistrar,
                      icon: enviando
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.save_rounded),
                      label: Text(
                        enviando ? 'Registrando...' : 'Registrar venta',
                      ),
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

class _Tarjeta extends StatelessWidget {
  _Tarjeta({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.tarjeta,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.1)),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _FilaResumen extends StatelessWidget {
  _FilaResumen({required this.etiqueta, required this.valor, this.color});

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
          Text(
            etiqueta,
            style: TextStyle(fontSize: 12.5, color: AppColors.textoSecundario),
          ),
          Text(
            valor,
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w700,
              color: color ?? AppColors.textoPrincipal,
            ),
          ),
        ],
      ),
    );
  }
}

class _FilaCarrito extends StatelessWidget {
  _FilaCarrito({
    required this.item,
    required this.formatoMoneda,
    required this.onQuitar,
    required this.onCambiarCantidad,
    required this.onCambiarPrecio,
    required this.onVerDetalle,
  });

  final ItemCarrito item;
  final NumberFormat formatoMoneda;
  final VoidCallback onQuitar;
  final ValueChanged<int> onCambiarCantidad;
  final ValueChanged<double> onCambiarPrecio;
  final VoidCallback onVerDetalle;

  static Color get _azulVerdoso => AppColors.acAguamarina;

  @override
  Widget build(BuildContext context) {
    final distinto = item.precioFinal != item.precioOriginal;
    final colorDiferencia = item.precioFinal > item.precioOriginal
        ? AppColors.error
        : AppColors.acAmbar;
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.fromLTRB(12, 6, 6, 12),
      decoration: BoxDecoration(
        color: AppColors.tarjeta,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.borde),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Expanded(
                // Todo el espacio del nombre se puede tocar para ver el detalle.
                child: InkWell(
                  onTap: onVerDetalle,
                  borderRadius: BorderRadius.circular(8),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Row(
                      children: <Widget>[
                        Flexible(
                          child: Text(
                            item.nombre,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 14,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Icon(
                          Icons.info_outline_rounded,
                          size: 18,
                          color: AppColors.acAguamarina,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Quitar',
                onPressed: onQuitar,
                icon: Icon(
                  Icons.delete_outline,
                  color: AppColors.error,
                  size: 22,
                ),
              ),
            ],
          ),
          Row(
            children: <Widget>[
              _BotonPaso(
                icono: Icons.remove,
                onTap: item.cantidad > 1
                    ? () => onCambiarCantidad(item.cantidad - 1)
                    : null,
              ),
              SizedBox(
                width: 34,
                child: Text(
                  '${item.cantidad}',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                  ),
                ),
              ),
              _BotonPaso(
                icono: Icons.add,
                onTap: item.cantidad < item.stockDisponible
                    ? () => onCambiarCantidad(item.cantidad + 1)
                    : null,
              ),
              const Spacer(),
              Padding(
                padding: const EdgeInsets.only(right: 6),
                child: Text(
                  formatoMoneda.format(item.subtotal),
                  style: TextStyle(
                    fontWeight: FontWeight.w900,
                    fontSize: 17,
                    color: AppColors.verdeOscuro,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          // Precio unitario: es lo que más se toca, así que va en una caja
          // destacada, con el lápiz y el número grande.
          Padding(
            padding: const EdgeInsets.only(right: 6),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: AppColors.tintAguamarina,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFF3E9B94), width: 1.5),
              ),
              child: Row(
                children: <Widget>[
                  Icon(Icons.edit_outlined, size: 18, color: _azulVerdoso),
                  const SizedBox(width: 8),
                  Text(
                    'Precio unitario',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: _azulVerdoso,
                    ),
                  ),
                  const SizedBox(width: 10),
                  // El número va dentro de un campo (fondo blanco y borde)
                  // para que se note que se puede editar.
                  Expanded(
                    child: Container(
                      margin: const EdgeInsets.symmetric(vertical: 6),
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                      decoration: BoxDecoration(
                        color: AppColors.tarjeta,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: const Color(0xFF3E9B94)),
                      ),
                      child: TextFormField(
                        // La clave incluye el precio de catálogo: si el admin lo
                        // actualiza, el campo se redibuja con el valor vigente.
                        key: ValueKey<String>(
                          'precio-${item.idProducto}-${item.precioOriginal}',
                        ),
                        initialValue: item.precioFinal.toStringAsFixed(2),
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        textAlign: TextAlign.end,
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w900,
                          color: AppColors.textoPrincipal,
                        ),
                        decoration: InputDecoration(
                          isDense: true,
                          filled: false,
                          prefixText: 'Bs. ',
                          prefixStyle: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w900,
                            color: _azulVerdoso,
                          ),
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                          contentPadding: EdgeInsets.symmetric(vertical: 10),
                        ),
                        onChanged: (valor) {
                          final precio = double.tryParse(
                            valor.replaceAll(',', '.'),
                          );
                          if (precio != null && precio >= 0)
                            onCambiarPrecio(precio);
                        },
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: <Widget>[
              Text(
                'Catálogo Bs. ${item.precioOriginal.toStringAsFixed(2)}',
                style: TextStyle(
                  fontSize: 11.5,
                  color: AppColors.textoSecundario,
                ),
              ),
              if (distinto && item.precioOriginal > 0) ...<Widget>[
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: colorDiferencia.withValues(alpha: 0.14),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    _textoDiferencia(item),
                    style: TextStyle(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w800,
                      color: colorDiferencia,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

/// Diferencia con el precio de catálogo, con signo: "−10.0%" o "+5.0%".
String _textoDiferencia(ItemCarrito item) {
  final porcentaje =
      ((item.precioFinal - item.precioOriginal) / item.precioOriginal) * 100;
  final signo = porcentaje < 0 ? '−' : '+';
  return '$signo${porcentaje.abs().toStringAsFixed(1)}%';
}

class _BotonPaso extends StatelessWidget {
  _BotonPaso({required this.icono, required this.onTap});

  final IconData icono;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    // El área tocable mide 48x48 (mínimo declarado), aunque el ícono se
    // vea del mismo tamaño de siempre dentro de esa zona.
    return SizedBox(
      width: 48,
      height: 48,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Center(
          child: Container(
            width: 28,
            height: 28,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: onTap != null ? AppColors.tarjeta : AppColors.fondo,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.borde),
            ),
            child: Icon(
              icono,
              size: 16,
              color: onTap != null ? AppColors.verdeOscuro : Colors.grey,
            ),
          ),
        ),
      ),
    );
  }
}

/// Selector de producto: chips de categoría siempre a la vista, buscador que
/// se abre con la lupa, y el detalle del producto dentro de la misma hoja.
/// Tocar la fila lo agrega directo; la cajita de la derecha abre el detalle.
class _SelectorProductoSheet extends StatefulWidget {
  _SelectorProductoSheet({required this.catalogo});

  final List<ProductoCatalogo> catalogo;

  @override
  State<_SelectorProductoSheet> createState() => _SelectorProductoSheetState();
}

class _SelectorProductoSheetState extends State<_SelectorProductoSheet> {
  String _busqueda = '';
  String? _categoria; // null = todas
  ProductoCatalogo? _detalle;
  final _formatoMoneda = NumberFormat.currency(
    locale: 'es_BO',
    symbol: 'Bs. ',
    decimalDigits: 2,
  );

  /// Categorías de los productos que se pueden vender ahora, ordenadas.
  List<String> get _categorias {
    final nombres =
        widget.catalogo
            .where((p) => !p.agotado)
            .map((p) => p.nombreCategoria)
            .whereType<String>()
            .where((n) => n.trim().isNotEmpty)
            .toSet()
            .toList()
          ..sort();
    return nombres;
  }

  List<ProductoCatalogo> get _filtrados {
    final q = _busqueda.trim().toLowerCase();
    return widget.catalogo.where((p) {
      if (p.agotado) return false;
      if (_categoria != null && p.nombreCategoria != _categoria) return false;
      return q.isEmpty || p.nombre.toLowerCase().contains(q);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final detalle = _detalle;
    if (detalle != null) {
      return _HojaSelector(
        titulo: 'Detalle del producto',
        child: _DetalleProductoVenta(
          producto: detalle,
          formatoMoneda: _formatoMoneda,
          onAtras: () => setState(() => _detalle = null),
          onAgregar: () => Navigator.of(context).pop(detalle),
        ),
      );
    }

    final lista = _filtrados;
    return _HojaSelector(
      titulo: 'Elegir producto',
      hintBuscador: 'Buscar por nombre...',
      onBuscar: (v) => setState(() => _busqueda = v),
      chips: _CategoriasRapidas(
        categorias: _categorias,
        seleccionada: _categoria,
        onSeleccionar: (c) => setState(() => _categoria = c),
      ),
      child: lista.isEmpty
          ? const Center(
              child: Padding(
                padding: EdgeInsets.all(20),
                child: Text('Sin productos disponibles para esta búsqueda.'),
              ),
            )
          : ListView.separated(
              itemCount: lista.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final p = lista[index];
                return Material(
                  color: AppColors.tarjeta,
                  borderRadius: BorderRadius.circular(14),
                  child: InkWell(
                    onTap: () => Navigator.of(context).pop(p),
                    borderRadius: BorderRadius.circular(14),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                          color: AppColors.verdeOscuro.withValues(alpha: 0.12),
                        ),
                      ),
                      child: Row(
                        children: <Widget>[
                          _MiniImagen(url: p.imagenUrl, tamano: 52),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Text(
                                  p.nombre,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 13.5,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  'Stock: ${p.cantidadDisponible}',
                                  style: TextStyle(
                                    fontSize: 11.5,
                                    color: AppColors.textoSecundario,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 6),
                          Text(
                            _formatoMoneda.format(p.precioVenta),
                            style: TextStyle(
                              color: AppColors.verdeOscuro,
                              fontWeight: FontWeight.w800,
                              fontSize: 13,
                            ),
                          ),
                          const SizedBox(width: 8),
                          InkWell(
                            onTap: () => setState(() => _detalle = p),
                            borderRadius: BorderRadius.circular(10),
                            child: Container(
                              width: 40,
                              height: 40,
                              decoration: BoxDecoration(
                                color: AppColors.tintAguamarina,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(
                                  color: const Color(0xFF3E9B94),
                                ),
                              ),
                              child: Icon(
                                Icons.visibility_outlined,
                                size: 20,
                                color: AppColors.acAguamarina,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
    );
  }
}

/// Fila de chips con las categorías (más "Todas"): se desplaza de lado a lado.
class _CategoriasRapidas extends StatelessWidget {
  _CategoriasRapidas({
    required this.categorias,
    required this.seleccionada,
    required this.onSeleccionar,
  });

  final List<String> categorias;
  final String? seleccionada;
  final ValueChanged<String?> onSeleccionar;

  @override
  Widget build(BuildContext context) {
    Widget chip(String texto, bool sel, VoidCallback onTap) {
      return Padding(
        padding: const EdgeInsets.only(right: 8),
        child: ChoiceChip(
          label: Text(texto),
          selected: sel,
          showCheckmark: false,
          selectedColor: AppColors.verdeOscuro,
          backgroundColor: AppColors.tarjeta,
          labelStyle: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w700,
            color: sel ? Colors.white : AppColors.textoPrincipal,
          ),
          onSelected: (_) => onTap(),
        ),
      );
    }

    return SizedBox(
      height: 40,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: <Widget>[
          chip('Todas', seleccionada == null, () => onSeleccionar(null)),
          for (final c in categorias)
            chip(c, seleccionada == c, () => onSeleccionar(c)),
        ],
      ),
    );
  }
}

/// Imagen cuadrada del producto (o el ícono de cama si no tiene foto).
class _MiniImagen extends StatelessWidget {
  _MiniImagen({required this.url, required this.tamano});

  final String? url;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final radio = BorderRadius.circular(10);
    final vacio = Container(
      width: tamano,
      height: tamano,
      decoration: BoxDecoration(color: AppColors.fondo, borderRadius: radio),
      child: Icon(Icons.bed_outlined, color: AppColors.textoSecundario),
    );
    if (url == null) return vacio;
    return ClipRRect(
      borderRadius: radio,
      child: Image.network(
        url!,
        width: tamano,
        height: tamano,
        fit: BoxFit.cover,
        cacheWidth: tamano <= 100 ? kAnchoMiniatura : (tamano * 2.5).round(),
        errorBuilder: (_, _, _) => vacio,
      ),
    );
  }
}

/// Detalle del producto dentro del selector, con Atrás (vuelve a la lista) y
/// Agregar (lo pone en la venta).
class _DetalleProductoVenta extends StatelessWidget {
  _DetalleProductoVenta({
    required this.producto,
    required this.formatoMoneda,
    required this.onAtras,
    this.onAgregar,
    this.textoAtras = 'Atrás',
  });

  final ProductoCatalogo producto;
  final NumberFormat formatoMoneda;
  final VoidCallback onAtras;

  /// Si es null (detalle de un producto ya agregado) solo hay un botón.
  final VoidCallback? onAgregar;
  final String textoAtras;

  @override
  Widget build(BuildContext context) {
    // Sin "Agregar" (ventana flotante): se ajusta al contenido, no ocupa toda la hoja.
    final soloOk = onAgregar == null;
    return Column(
      mainAxisSize: soloOk ? MainAxisSize.min : MainAxisSize.max,
      children: <Widget>[
        Flexible(
          fit: soloOk ? FlexFit.loose : FlexFit.tight,
          child: ListView(
            shrinkWrap: soloOk,
            children: <Widget>[
              Center(child: _MiniImagen(url: producto.imagenUrl, tamano: 160)),
              const SizedBox(height: 14),
              Text(
                producto.nombre,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                [
                  if (producto.nombreCategoria != null)
                    producto.nombreCategoria!,
                  'SKU: ${producto.sku}',
                ].join('  ·  '),
                style: TextStyle(
                  fontSize: 12,
                  color: AppColors.textoSecundario,
                ),
              ),
              const SizedBox(height: 14),
              Row(
                children: <Widget>[
                  Expanded(
                    child: _DatoDetalle(
                      icono: Icons.payments_outlined,
                      etiqueta: 'Precio',
                      valor: formatoMoneda.format(producto.precioVenta),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _DatoDetalle(
                      icono: Icons.inventory_2_outlined,
                      etiqueta: 'Stock',
                      valor: '${producto.cantidadDisponible} unidades',
                    ),
                  ),
                ],
              ),
              if (producto.descripcion.isNotEmpty) ...<Widget>[
                const SizedBox(height: 14),
                const Text(
                  'Descripción',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                ),
                const SizedBox(height: 4),
                Text(
                  producto.descripcion,
                  style: TextStyle(
                    fontSize: 13,
                    color: AppColors.textoSecundario,
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 10),
        if (onAgregar == null)
          Center(
            child: FilledButton(
              onPressed: onAtras,
              style: FilledButton.styleFrom(
                minimumSize: const Size(96, 44),
                padding: const EdgeInsets.symmetric(horizontal: 28),
              ),
              child: Text(textoAtras),
            ),
          )
        else
          Row(
            children: <Widget>[
              Expanded(
                flex: 2,
                child: OutlinedButton.icon(
                  onPressed: onAtras,
                  icon: const Icon(Icons.arrow_back_rounded, size: 18),
                  label: Text(textoAtras),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 3,
                child: FilledButton.icon(
                  onPressed: onAgregar,
                  icon: const Icon(Icons.add_shopping_cart_rounded),
                  label: const Text('Agregar'),
                ),
              ),
            ],
          ),
      ],
    );
  }
}

class _DatoDetalle extends StatelessWidget {
  _DatoDetalle({
    required this.icono,
    required this.etiqueta,
    required this.valor,
  });

  final IconData icono;
  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.tarjeta,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: <Widget>[
          Icon(icono, size: 20, color: AppColors.verdeOscuro),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  etiqueta,
                  style: TextStyle(
                    fontSize: 11,
                    color: AppColors.textoSecundario,
                  ),
                ),
                Text(
                  valor,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Selector de cliente: mismo patrón, sobre la lista ya cargada.
/// "Cel: 71234567 · CI: 9392342", o null si el cliente no tiene ninguno de los dos.
String? _datosCliente(Cliente c) {
  final partes = <String>[
    if (c.telefono != null && c.telefono!.trim().isNotEmpty)
      'Cel: ${c.telefono!.trim()}',
    if (c.nitCi != null && c.nitCi!.trim().isNotEmpty) 'CI: ${c.nitCi!.trim()}',
  ];
  return partes.isEmpty ? null : partes.join(' · ');
}

class _SelectorClienteSheet extends StatefulWidget {
  _SelectorClienteSheet({required this.clientes});

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
        .where(
          (c) =>
              c.nombreCompleto.toLowerCase().contains(q) ||
              (c.telefono ?? '').contains(q),
        )
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    return _HojaSelector(
      titulo: 'Elegir cliente',
      hintBuscador: 'Buscar por nombre o teléfono...',
      onBuscar: (v) => setState(() => _busqueda = v),
      child: _filtrados.isEmpty
          ? const Center(
              child: Padding(
                padding: EdgeInsets.all(20),
                child: Text('No encontrado. Registralo como cliente nuevo.'),
              ),
            )
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
                      color: AppColors.tarjeta,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(
                        color: AppColors.verdeOscuro.withValues(alpha: 0.12),
                      ),
                    ),
                    child: Row(
                      children: <Widget>[
                        Icon(
                          Icons.person_outline,
                          color: AppColors.verdeOscuro,
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                c.nombreCompleto,
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13.5,
                                ),
                              ),
                              Text(
                                c.telefono ?? 'Sin teléfono',
                                style: TextStyle(
                                  fontSize: 11.5,
                                  color: AppColors.textoSecundario,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}

/// Contenedor común de los selectores: hoja con asa, título y una lupa. La
/// lupa abre el campo de búsqueda en el lugar del título; cerrarlo lo deja
/// como estaba. Los chips (si hay) quedan siempre a la vista.
class _HojaSelector extends StatefulWidget {
  _HojaSelector({
    required this.titulo,
    required this.child,
    this.hintBuscador,
    this.onBuscar,
    this.chips,
  });

  final String titulo;
  final Widget child;
  final String? hintBuscador;

  /// Si es null, la hoja no tiene buscador (por ejemplo en el detalle).
  final ValueChanged<String>? onBuscar;
  final Widget? chips;

  @override
  State<_HojaSelector> createState() => _HojaSelectorState();
}

class _HojaSelectorState extends State<_HojaSelector> {
  bool _buscando = false;

  void _cerrarBusqueda() {
    setState(() => _buscando = false);
    widget.onBuscar?.call('');
  }

  @override
  Widget build(BuildContext context) {
    final puedeBuscar = widget.onBuscar != null;
    return Container(
      height: MediaQuery.of(context).size.height * 0.8,
      padding: const EdgeInsets.fromLTRB(18, 14, 18, 16),
      decoration: BoxDecoration(
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
              decoration: BoxDecoration(
                color: AppColors.borde,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            height: 52,
            child: _buscando && puedeBuscar
                ? Row(
                    children: <Widget>[
                      Expanded(
                        child: TextField(
                          autofocus: true,
                          decoration: InputDecoration(
                            hintText: widget.hintBuscador,
                            prefixIcon: const Icon(Icons.search),
                          ),
                          onChanged: widget.onBuscar,
                        ),
                      ),
                      IconButton(
                        tooltip: 'Cerrar búsqueda',
                        icon: const Icon(Icons.close_rounded),
                        onPressed: _cerrarBusqueda,
                      ),
                    ],
                  )
                : Row(
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          widget.titulo,
                          style: const TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      if (puedeBuscar)
                        IconButton(
                          tooltip: 'Buscar',
                          icon: const Icon(Icons.search_rounded),
                          onPressed: () => setState(() => _buscando = true),
                        ),
                      IconButton(
                        tooltip: 'Cerrar',
                        icon: const Icon(Icons.close_rounded),
                        onPressed: () => Navigator.of(context).pop(),
                      ),
                    ],
                  ),
          ),
          if (widget.chips != null) ...<Widget>[
            const SizedBox(height: 6),
            widget.chips!,
          ],
          const SizedBox(height: 10),
          Expanded(child: widget.child),
        ],
      ),
    );
  }
}
