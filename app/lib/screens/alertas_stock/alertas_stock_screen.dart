import 'package:flutter/material.dart';

import '../../data/api_exception.dart';
import '../../data/catalogo_repository.dart';
import '../../models/producto_catalogo.dart';
import '../../theme/app_colors.dart';

enum _EstadoAlertas { cargando, conDatos, vacio, error }

/// Productos bajo su stock mínimo. Mismo patrón que CatalogoScreen: mismo
/// repositorio, mismos cuatro estados, mismo endpoint con soloBajoMinimo=true.
class AlertasStockScreen extends StatefulWidget {
  const AlertasStockScreen({super.key, required this.token});

  final String token;

  @override
  State<AlertasStockScreen> createState() => _AlertasStockScreenState();
}

class _AlertasStockScreenState extends State<AlertasStockScreen> {
  final _catalogoRepository = CatalogoRepository();

  _EstadoAlertas _estado = _EstadoAlertas.cargando;
  List<ProductoCatalogo> _productos = <ProductoCatalogo>[];
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _cargarAlertas();
  }

  Future<void> _cargarAlertas() async {
    setState(() {
      _estado = _EstadoAlertas.cargando;
      _errorMensaje = null;
    });

    try {
      final productos = await _catalogoRepository.obtenerBajoMinimo(widget.token);
      if (!mounted) return;
      setState(() {
        _productos = productos;
        _estado = productos.isEmpty ? _EstadoAlertas.vacio : _EstadoAlertas.conDatos;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = error.mensaje;
        _estado = _EstadoAlertas.error;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = 'No se pudieron cargar las alertas de stock.';
        _estado = _EstadoAlertas.error;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Alertas de stock')),
      body: RefreshIndicator(
        color: AppColors.verdeOscuro,
        onRefresh: _cargarAlertas,
        child: switch (_estado) {
          _EstadoAlertas.cargando => const _CentroCargando(),
          _EstadoAlertas.error => _CentroError(mensaje: _errorMensaje, onReintentar: _cargarAlertas),
          _EstadoAlertas.vacio => const _CentroVacio(),
          _EstadoAlertas.conDatos => ListView.separated(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 20),
              itemCount: _productos.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) => _TarjetaAlerta(producto: _productos[index]),
            ),
        },
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

/// A diferencia de un catálogo vacío (no hay nada que mostrar), acá "vacío"
/// es la mejor noticia posible: ningún producto está bajo su mínimo. El
/// texto lo dice así, no como si faltaran datos.
class _CentroVacio extends StatelessWidget {
  const _CentroVacio();

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: const <Widget>[
        SizedBox(height: 80),
        Icon(Icons.check_circle_outline_rounded, color: AppColors.verdeOscuro, size: 44),
        SizedBox(height: 12),
        Text(
          'El stock está en orden',
          textAlign: TextAlign.center,
          style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.textoPrincipal, fontSize: 16),
        ),
        SizedBox(height: 4),
        Text(
          'Ningún producto está por debajo de su mínimo.',
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.textoSecundario),
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
          mensaje ?? 'No se pudieron cargar las alertas de stock.',
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

class _TarjetaAlerta extends StatelessWidget {
  const _TarjetaAlerta({required this.producto});

  final ProductoCatalogo producto;

  @override
  Widget build(BuildContext context) {
    final agotado = producto.agotado;
    final colorAcento = agotado ? AppColors.error : const Color(0xFFD97706);

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: colorAcento.withValues(alpha: 0.25)),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Row(
        children: <Widget>[
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: colorAcento.withValues(alpha: 0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(
              agotado ? Icons.remove_shopping_cart_outlined : Icons.warning_amber_rounded,
              color: colorAcento,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  producto.nombre,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
                const SizedBox(height: 2),
                Text(
                  agotado ? 'Sin stock' : 'Stock bajo',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colorAcento),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: <Widget>[
              Text(
                '${producto.cantidadDisponible}',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: colorAcento),
              ),
              Text(
                'mínimo ${producto.stockMinimo}',
                style: const TextStyle(fontSize: 11, color: AppColors.textoSecundario),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
