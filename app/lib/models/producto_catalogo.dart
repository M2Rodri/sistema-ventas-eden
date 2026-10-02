import '../config/api_config.dart';

/// Producto para la pantalla de Catálogo: nombre, precio, stock y lo mínimo
/// para mostrarlo en una tarjeta y en su detalle.
///
/// Sale de GET /api/v1/inventario/catalogo (CatalogoProductoResponse en el
/// backend), que ya cruza producto + inventario en el servidor.
class ProductoCatalogo {
  const ProductoCatalogo({
    required this.id,
    required this.sku,
    required this.nombre,
    required this.descripcion,
    required this.nombreCategoria,
    required this.precioVenta,
    required this.imagenUrl,
    required this.cantidadDisponible,
    required this.stockMinimo,
  });

  final int id;
  final String sku;
  final String nombre;
  final String descripcion;
  final String? nombreCategoria;
  final double precioVenta;
  final String? imagenUrl;
  final int cantidadDisponible;
  final int stockMinimo;

  bool get agotado => cantidadDisponible <= 0;
  bool get bajoStockMinimo => stockMinimo > 0 && cantidadDisponible > 0 && cantidadDisponible <= stockMinimo;

  factory ProductoCatalogo.desdeApi(Map<String, dynamic> json) {
    final imagenes = json['imagenes'] as List<dynamic>? ?? const <dynamic>[];
    String? imagenUrl;
    if (imagenes.isNotEmpty) {
      final principal = imagenes.cast<Map<String, dynamic>>().firstWhere(
            (img) => img['esPrincipal'] == true,
            orElse: () => imagenes.first as Map<String, dynamic>,
          );
      final urlImagen = principal['urlImagen'] as String?;
      if (urlImagen != null && urlImagen.isNotEmpty) {
        // Las fotos nuevas vienen con la URL completa (Supabase); las viejas, relativas al backend.
        imagenUrl = urlImagen.startsWith('http') ? urlImagen : '${ApiConfig.baseUrl}$urlImagen';
      }
    }

    return ProductoCatalogo(
      id: json['id'] as int,
      sku: json['sku'] as String? ?? '',
      nombre: json['nombre'] as String? ?? '',
      descripcion: json['descripcion'] as String? ?? '',
      nombreCategoria: json['nombreCategoria'] as String?,
      precioVenta: (json['precioVenta'] as num?)?.toDouble() ?? 0.0,
      imagenUrl: imagenUrl,
      cantidadDisponible: (json['cantidadDisponible'] as num?)?.toInt() ?? 0,
      stockMinimo: (json['stockMinimo'] as num?)?.toInt() ?? 0,
    );
  }
}
