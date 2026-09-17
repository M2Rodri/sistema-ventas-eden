import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_exception.dart';
import '../../data/catalogo_repository.dart';
import '../../models/producto_catalogo.dart';
import '../../theme/app_colors.dart';

enum _EstadoCatalogo { cargando, conDatos, vacio, error }

/// Catálogo de consulta: precio y stock de cada producto. No edita nada,
/// es para mirar en el salón o con el cliente en el teléfono.
class CatalogoScreen extends StatefulWidget {
  const CatalogoScreen({super.key, required this.token});

  final String token;

  @override
  State<CatalogoScreen> createState() => _CatalogoScreenState();
}

class _CatalogoScreenState extends State<CatalogoScreen> {
  final _catalogoRepository = CatalogoRepository();
  final _busquedaCtrl = TextEditingController();
  final _formatoMoneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);

  _EstadoCatalogo _estado = _EstadoCatalogo.cargando;
  List<ProductoCatalogo> _productos = <ProductoCatalogo>[];
  String? _errorMensaje;
  String _busqueda = '';

  @override
  void initState() {
    super.initState();
    _cargarCatalogo();
  }

  @override
  void dispose() {
    _busquedaCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarCatalogo() async {
    setState(() {
      _estado = _EstadoCatalogo.cargando;
      _errorMensaje = null;
    });

    try {
      final productos = await _catalogoRepository.obtenerCatalogo(widget.token);
      if (!mounted) return;
      setState(() {
        _productos = productos;
        _estado = productos.isEmpty ? _EstadoCatalogo.vacio : _EstadoCatalogo.conDatos;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = error.mensaje;
        _estado = _EstadoCatalogo.error;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = 'No se pudo cargar el catálogo.';
        _estado = _EstadoCatalogo.error;
      });
    }
  }

  List<ProductoCatalogo> get _filtrados {
    final q = _busqueda.trim().toLowerCase();
    if (q.isEmpty) return _productos;
    return _productos.where((p) => p.nombre.toLowerCase().contains(q)).toList();
  }

  void _abrirDetalle(ProductoCatalogo producto) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _DetalleProductoSheet(producto: producto, formatoMoneda: _formatoMoneda),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Catálogo')),
      body: RefreshIndicator(
        color: AppColors.verdeOscuro,
        onRefresh: _cargarCatalogo,
        child: _estado == _EstadoCatalogo.cargando
            ? const _CentroCargando()
            : _estado == _EstadoCatalogo.error
                ? _CentroError(mensaje: _errorMensaje, onReintentar: _cargarCatalogo)
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
                      Expanded(
                        child: _estado == _EstadoCatalogo.vacio
                            ? const _CentroVacio(mensaje: 'Todavía no hay productos activos.')
                            : _filtrados.isEmpty
                                ? const _CentroVacio(mensaje: 'Ningún producto coincide con la búsqueda.')
                                : ListView.separated(
                                    physics: const AlwaysScrollableScrollPhysics(),
                                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
                                    itemCount: _filtrados.length,
                                    separatorBuilder: (_, _) => const SizedBox(height: 10),
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
        const Icon(Icons.inventory_2_outlined, color: AppColors.textoSecundario, size: 40),
        const SizedBox(height: 10),
        Text(
          mensaje,
          textAlign: TextAlign.center,
          style: const TextStyle(color: AppColors.textoSecundario),
        ),
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
          mensaje ?? 'No se pudo cargar el catálogo.',
          textAlign: TextAlign.center,
          style: const TextStyle(color: AppColors.textoPrincipal),
        ),
        const SizedBox(height: 14),
        Center(
          child: OutlinedButton(onPressed: onReintentar, child: const Text('Reintentar')),
        ),
      ],
    );
  }
}

class _TarjetaProducto extends StatelessWidget {
  const _TarjetaProducto({
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
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
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
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
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
                        style: const TextStyle(
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
  const _BadgeCategoria({required this.texto});

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
        style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.verdeSuave),
      ),
    );
  }
}

class _BadgeStock extends StatelessWidget {
  const _BadgeStock({required this.producto});

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
      color = const Color(0xFFD97706);
    } else {
      texto = 'Stock: ${producto.cantidadDisponible}';
      color = AppColors.verdeOscuro;
    }
    return Text(texto, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: color));
  }
}

class _ImagenProducto extends StatelessWidget {
  const _ImagenProducto({required this.url, required this.tamano});

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
        child: const Icon(Icons.bed_outlined, color: AppColors.textoSecundario),
      );
    }
    return ClipRRect(
      borderRadius: radius,
      child: Image.network(
        url!,
        width: tamano,
        height: tamano,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => Container(
          width: tamano,
          height: tamano,
          color: AppColors.fondo,
          child: const Icon(Icons.bed_outlined, color: AppColors.textoSecundario),
        ),
      ),
    );
  }
}

class _DetalleProductoSheet extends StatelessWidget {
  const _DetalleProductoSheet({required this.producto, required this.formatoMoneda});

  final ProductoCatalogo producto;
  final NumberFormat formatoMoneda;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
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
                decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Center(child: _ImagenProducto(url: producto.imagenUrl, tamano: 140)),
            const SizedBox(height: 16),
            Text(
              producto.nombre,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textoPrincipal),
            ),
            const SizedBox(height: 4),
            Row(
              children: <Widget>[
                if (producto.nombreCategoria != null) _BadgeCategoria(texto: producto.nombreCategoria!),
                const SizedBox(width: 8),
                Text('SKU: ${producto.sku}', style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
              ],
            ),
            const SizedBox(height: 16),
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
                    valor: producto.agotado ? 'Agotado' : '${producto.cantidadDisponible} unidades',
                  ),
                ),
              ],
            ),
            if (producto.descripcion.isNotEmpty) ...<Widget>[
              const SizedBox(height: 16),
              const Text('Descripción', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 4),
              Text(producto.descripcion, style: TextStyle(fontSize: 13, color: Colors.grey.shade700)),
            ],
          ],
        ),
      ),
    );
  }
}

class _MetricaDetalle extends StatelessWidget {
  const _MetricaDetalle({required this.icono, required this.etiqueta, required this.valor});

  final IconData icono;
  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFE8F4EC),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(icono, color: AppColors.verdeOscuro, size: 18),
          const SizedBox(height: 6),
          Text(
            valor,
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal),
          ),
          const SizedBox(height: 2),
          Text(etiqueta, style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario)),
        ],
      ),
    );
  }
}
