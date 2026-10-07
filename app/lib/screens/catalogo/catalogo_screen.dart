import 'dart:async';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_client.dart';
import '../../data/precarga_datos.dart';
import '../../data/api_exception.dart';
import '../../data/catalogo_repository.dart';
import '../../models/producto_catalogo.dart';
import '../../theme/app_colors.dart';

enum _EstadoCatalogo { cargando, conDatos, vacio, error }

/// Catálogo de consulta: precio y stock de cada producto. No edita nada,
/// es para mirar en el salón o con el cliente en el teléfono.
class CatalogoScreen extends StatefulWidget {
  CatalogoScreen({super.key, required this.token});

  final String token;

  @override
  State<CatalogoScreen> createState() => _CatalogoScreenState();
}

class _CatalogoScreenState extends State<CatalogoScreen> {
  final _catalogoRepository = CatalogoRepository();
  final _busquedaCtrl = TextEditingController();
  final _formatoMoneda = NumberFormat.currency(
    locale: 'es_BO',
    symbol: 'Bs. ',
    decimalDigits: 2,
  );

  _EstadoCatalogo _estado = _EstadoCatalogo.cargando;
  List<ProductoCatalogo> _productos = <ProductoCatalogo>[];
  String? _errorMensaje;
  String _busqueda = '';
  String? _categoria; // null = todas
  StreamSubscription<String>? _suscripcion;

  @override
  void initState() {
    super.initState();
    _cargarCatalogo();
    // Si llegan datos nuevos por detrás, el catálogo se corrige solo, sin spinner.
    _suscripcion = ApiClient.actualizaciones.listen((ruta) {
      if (ruta == '/api/v1/inventario/catalogo')
        _cargarCatalogo(silencioso: true);
    });
  }

  @override
  void dispose() {
    _suscripcion?.cancel();
    _busquedaCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarCatalogo({bool silencioso = false}) async {
    if (!silencioso) {
      setState(() {
        _estado = _EstadoCatalogo.cargando;
        _errorMensaje = null;
      });
    }

    try {
      final productos = await _catalogoRepository.obtenerCatalogo(widget.token);
      if (!mounted) return;
      setState(() {
        _productos = productos;
        _estado = productos.isEmpty
            ? _EstadoCatalogo.vacio
            : _EstadoCatalogo.conDatos;
      });
    } on ApiException catch (error) {
      if (!mounted || silencioso) return;
      setState(() {
        _errorMensaje = error.mensaje;
        _estado = _EstadoCatalogo.error;
      });
    } catch (_) {
      if (!mounted || silencioso) return;
      setState(() {
        _errorMensaje = 'No se pudo cargar el catálogo.';
        _estado = _EstadoCatalogo.error;
      });
    }
  }

  /// Categorías de los productos del catálogo, ordenadas.
  List<String> get _categorias =>
      (_productos
            .map((p) => p.nombreCategoria)
            .whereType<String>()
            .where((n) => n.trim().isNotEmpty)
            .toSet()
            .toList())
        ..sort();

  List<ProductoCatalogo> get _filtrados {
    final q = _busqueda.trim().toLowerCase();
    return _productos.where((p) {
      if (_categoria != null && p.nombreCategoria != _categoria) return false;
      return q.isEmpty || p.nombre.toLowerCase().contains(q);
    }).toList();
  }

  void _abrirDetalle(ProductoCatalogo producto) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => DetalleProductoSheet(
        producto: producto,
        formatoMoneda: _formatoMoneda,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Catálogo')),
      body: RefreshIndicator(
        color: AppColors.verdeOscuro,
        onRefresh: () {
          ApiClient.vaciarCache();
          return _cargarCatalogo();
        },
        child: _estado == _EstadoCatalogo.cargando
            ? _CentroCargando()
            : _estado == _EstadoCatalogo.error
            ? _CentroError(
                mensaje: _errorMensaje,
                onReintentar: _cargarCatalogo,
              )
            : Column(
                children: <Widget>[
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
                    child: TextField(
                      controller: _busquedaCtrl,
                      decoration: InputDecoration(
                        hintText: 'Buscar por nombre...',
                        prefixIcon: const Icon(Icons.search),
                        suffixIcon: _busqueda.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear, size: 18),
                                onPressed: () {
                                  _busquedaCtrl.clear();
                                  setState(() => _busqueda = '');
                                },
                              )
                            : null,
                      ),
                      onChanged: (v) => setState(() => _busqueda = v),
                    ),
                  ),
                  if (_categorias.isNotEmpty)
                    _ChipsCategoria(
                      categorias: _categorias,
                      seleccionada: _categoria,
                      onSeleccionar: (c) => setState(() => _categoria = c),
                    ),
                  Expanded(
                    child: _estado == _EstadoCatalogo.vacio
                        ? _CentroVacio(
                            mensaje: 'Todavía no hay productos activos.',
                          )
                        : _filtrados.isEmpty
                        ? _CentroVacio(
                            mensaje:
                                'Ningún producto coincide con la búsqueda.',
                          )
                        : ListView.separated(
                            physics: const AlwaysScrollableScrollPhysics(),
                            padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
                            itemCount: _filtrados.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 10),
                            itemBuilder: (context, index) {
                              final producto = _filtrados[index];
                              return _TarjetaProducto(
                                producto: producto,
                                formatoMoneda: _formatoMoneda,
                                onTap: () => _abrirDetalle(producto),
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

class _CentroCargando extends StatelessWidget {
  _CentroCargando();

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: <Widget>[
        SizedBox(height: 120),
        Center(child: CircularProgressIndicator(color: AppColors.verdeOscuro)),
      ],
    );
  }
}

class _CentroVacio extends StatelessWidget {
  _CentroVacio({required this.mensaje});

  final String mensaje;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: <Widget>[
        const SizedBox(height: 80),
        Icon(
          Icons.inventory_2_outlined,
          color: AppColors.textoSecundario,
          size: 40,
        ),
        const SizedBox(height: 10),
        Text(
          mensaje,
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.textoSecundario),
        ),
      ],
    );
  }
}

class _CentroError extends StatelessWidget {
  _CentroError({required this.mensaje, required this.onReintentar});

  final String? mensaje;
  final VoidCallback onReintentar;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: <Widget>[
        const SizedBox(height: 100),
        Icon(Icons.cloud_off_rounded, color: AppColors.error, size: 40),
        const SizedBox(height: 10),
        Text(
          mensaje ?? 'No se pudo cargar el catálogo.',
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.textoPrincipal),
        ),
        const SizedBox(height: 14),
        Center(
          child: OutlinedButton(
            onPressed: onReintentar,
            child: const Text('Reintentar'),
          ),
        ),
      ],
    );
  }
}

class _TarjetaProducto extends StatelessWidget {
  _TarjetaProducto({
    required this.producto,
    required this.formatoMoneda,
    required this.onTap,
  });

  final ProductoCatalogo producto;
  final NumberFormat formatoMoneda;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppColors.tarjeta,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: AppColors.verdeOscuro.withValues(alpha: 0.12),
          ),
          boxShadow: <BoxShadow>[
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.03),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            _ImagenProducto(url: producto.imagenUrl, tamano: 68),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          producto.nombre,
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                        ),
                      ),
                      if (producto.nombreCategoria != null) ...<Widget>[
                        const SizedBox(width: 6),
                        _BadgeCategoria(texto: producto.nombreCategoria!),
                      ],
                    ],
                  ),
                  const SizedBox(height: 6),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: <Widget>[
                      Text(
                        formatoMoneda.format(producto.precioVenta),
                        style: TextStyle(
                          color: AppColors.verdeOscuro,
                          fontWeight: FontWeight.w900,
                          fontSize: 15,
                        ),
                      ),
                      _BadgeStock(producto: producto),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BadgeCategoria extends StatelessWidget {
  _BadgeCategoria({required this.texto});

  final String texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.verdeSuave.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        texto,
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.bold,
          color: AppColors.verdeSuave,
        ),
      ),
    );
  }
}

class _BadgeStock extends StatelessWidget {
  _BadgeStock({required this.producto});

  final ProductoCatalogo producto;

  @override
  Widget build(BuildContext context) {
    final String texto;
    final Color color;
    if (producto.agotado) {
      texto = 'Agotado';
      color = AppColors.error;
    } else if (producto.bajoStockMinimo) {
      texto = 'Quedan ${producto.cantidadDisponible}';
      color = AppColors.acAmbar;
    } else {
      texto = 'Stock: ${producto.cantidadDisponible}';
      color = AppColors.verdeOscuro;
    }
    return Text(
      texto,
      style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: color),
    );
  }
}

class _ImagenProducto extends StatelessWidget {
  _ImagenProducto({required this.url, required this.tamano});

  final String? url;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final radius = BorderRadius.circular(12);
    if (url == null) {
      return Container(
        width: tamano,
        height: tamano,
        decoration: BoxDecoration(color: AppColors.fondo, borderRadius: radius),
        child: Icon(Icons.bed_outlined, color: AppColors.textoSecundario),
      );
    }
    return ClipRRect(
      borderRadius: radius,
      child: Image.network(
        url!,
        width: tamano,
        height: tamano,
        fit: BoxFit.cover,
        // Se decodifica a tamaño de miniatura (y se comparte con lo precargado):
        // mucho más liviano que dibujar la foto completa.
        cacheWidth: tamano <= 100 ? kAnchoMiniatura : (tamano * 2.5).round(),
        errorBuilder: (_, _, _) => Container(
          width: tamano,
          height: tamano,
          color: AppColors.fondo,
          child: Icon(Icons.bed_outlined, color: AppColors.textoSecundario),
        ),
      ),
    );
  }
}

/// Fila de chips con las categorías (más "Todas"); se desliza de lado a lado.
class _ChipsCategoria extends StatelessWidget {
  _ChipsCategoria({
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
      height: 42,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.fromLTRB(16, 0, 8, 6),
        children: <Widget>[
          chip('Todas', seleccionada == null, () => onSeleccionar(null)),
          for (final c in categorias)
            chip(c, seleccionada == c, () => onSeleccionar(c)),
        ],
      ),
    );
  }
}

/// Detalle del producto: foto, precio, stock, ficha técnica (solo los datos que
/// tenga registrados, como en la web) y descripción.
class DetalleProductoSheet extends StatelessWidget {
  DetalleProductoSheet({
    super.key,
    required this.producto,
    required this.formatoMoneda,
  });

  final ProductoCatalogo producto;
  final NumberFormat formatoMoneda;

  /// Mismos nombres y orden que la ficha de la web.
  List<(IconData, String, String)> get _ficha => <(IconData, String, String?)>[
    (Icons.sell_outlined, 'Marca', producto.marca),
    (Icons.style_outlined, 'Modelo', producto.modelo),
    (Icons.workspace_premium_outlined, 'Calidad', producto.calidad),
    (Icons.palette_outlined, 'Color', producto.color),
    (Icons.fitness_center_outlined, 'Firmeza', producto.firmeza),
    (Icons.layers_outlined, 'Material del núcleo', producto.materialNucleo),
    (
      Icons.chair_alt_outlined,
      'Material del armazón',
      producto.materialArmazon,
    ),
    (Icons.straighten_outlined, 'Medida', producto.dimensiones),
  ].where((d) => d.$3 != null).map((d) => (d.$1, d.$2, d.$3!)).toList();

  @override
  Widget build(BuildContext context) {
    final ficha = _ficha;
    return SafeArea(
      child: Container(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.88,
        ),
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
        decoration: BoxDecoration(
          color: AppColors.tarjeta,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
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
            const SizedBox(height: 14),
            Flexible(
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Center(
                      child: _ImagenProducto(
                        url: producto.imagenUrl,
                        tamano: 150,
                      ),
                    ),
                    const SizedBox(height: 14),
                    Text(
                      producto.nombre,
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: AppColors.textoPrincipal,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: <Widget>[
                        if (producto.nombreCategoria != null)
                          _BadgeCategoria(texto: producto.nombreCategoria!),
                        const SizedBox(width: 8),
                        Text(
                          'SKU: ${producto.sku}',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.textoSecundario,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: _MetricaDetalle(
                            icono: Icons.payments_outlined,
                            etiqueta: 'Precio',
                            valor: formatoMoneda.format(producto.precioVenta),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: _MetricaDetalle(
                            icono: Icons.inventory_2_outlined,
                            etiqueta: 'Stock disponible',
                            valor: producto.agotado
                                ? 'Agotado'
                                : '${producto.cantidadDisponible} unidades',
                          ),
                        ),
                      ],
                    ),
                    if (ficha.isNotEmpty) ...<Widget>[
                      const SizedBox(height: 18),
                      Text(
                        'FICHA TÉCNICA',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 0.8,
                          color: AppColors.textoSecundario,
                        ),
                      ),
                      const SizedBox(height: 8),
                      for (var i = 0; i < ficha.length; i += 2) ...<Widget>[
                        if (i > 0) const SizedBox(height: 8),
                        // IntrinsicHeight: las dos casillas de la fila miden lo
                        // mismo sin pedir altura infinita dentro del scroll.
                        IntrinsicHeight(
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: <Widget>[
                              Expanded(
                                child: _DatoFicha(
                                  icono: ficha[i].$1,
                                  etiqueta: ficha[i].$2,
                                  valor: ficha[i].$3,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: i + 1 < ficha.length
                                    ? _DatoFicha(
                                        icono: ficha[i + 1].$1,
                                        etiqueta: ficha[i + 1].$2,
                                        valor: ficha[i + 1].$3,
                                      )
                                    : const SizedBox.shrink(),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
                    if (producto.descripcion.isNotEmpty) ...<Widget>[
                      const SizedBox(height: 18),
                      Text(
                        'DESCRIPCIÓN',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 0.8,
                          color: AppColors.textoSecundario,
                        ),
                      ),
                      const SizedBox(height: 6),
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
            ),
          ],
        ),
      ),
    );
  }
}

/// Un dato de la ficha técnica: ícono, nombre chico y valor.
class _DatoFicha extends StatelessWidget {
  _DatoFicha({
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
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
      decoration: BoxDecoration(
        color: AppColors.fondo,
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
                    fontSize: 10.5,
                    color: AppColors.textoSecundario,
                  ),
                ),
                const SizedBox(height: 1),
                Text(
                  valor,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textoPrincipal,
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

class _MetricaDetalle extends StatelessWidget {
  _MetricaDetalle({
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
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
      decoration: BoxDecoration(
        color: AppColors.tintVerde,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: AppColors.verdeOscuro.withValues(alpha: 0.12),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(icono, color: AppColors.verdeOscuro, size: 18),
          const SizedBox(height: 6),
          Text(
            valor,
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w800,
              color: AppColors.textoPrincipal,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            etiqueta,
            style: TextStyle(fontSize: 11, color: AppColors.textoSecundario),
          ),
        ],
      ),
    );
  }
}
