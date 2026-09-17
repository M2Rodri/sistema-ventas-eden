import '../config/api_config.dart';

/// Producto para la pantalla de Catálogo: nombre, precio, stock y lo mínimo
/// para mostrarlo en una tarjeta y en su detalle.
///
/// No hay un endpoint que devuelva esto junto. Sale de cruzar dos:
///   GET /api/productos/activos (ProductoResponse) -> nombre, precioVenta,
///     sku, categoría, imágenes
///   GET /api/inventario (InventarioResponse)       -> cantidadDisponible
///     por idProducto
/// Ver CatalogoRepository.obtenerCatalogo, que arma esta lista.
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
  bool get bajoStockMinimo => cantidadDisponible > 0 && cantidadDisponible <= stockMinimo;

  /// [productoJson] es un elemento de GET /api/productos/activos.
  /// [cantidadDisponible] sale de cruzar ese id con GET /api/inventario; si
  /// el producto no tiene fila de inventario todavía, se asume 0.
  factory ProductoCatalogo.desdeApi(
    Map<String, dynamic> productoJson, {
    required int cantidadDisponible,
  }) {
    final imagenes = productoJson['imagenes'] as List<dynamic>? ?? const <dynamic>[];
    String? imagenUrl;
    if (imagenes.isNotEmpty) {
      final principal = imagenes.cast<Map<String, dynamic>>().firstWhere(
            (img) => img['esPrincipal'] == true,
            orElse: () => imagenes.first as Map<String, dynamic>,
          );
      final urlImagen = principal['urlImagen'] as String?;
      if (urlImagen != null && urlImagen.isNotEmpty) {
        imagenUrl = '${ApiConfig.baseUrl}$urlImagen';
      }
    }

    return ProductoCatalogo(
      id: productoJson['id'] as int,
      sku: productoJson['sku'] as String? ?? '',
      nombre: productoJson['nombre'] as String? ?? '',
      descripcion: productoJson['descripcion'] as String? ?? '',
      nombreCategoria: productoJson['nombreCategoria'] as String?,
      precioVenta: (productoJson['precioVenta'] as num?)?.toDouble() ?? 0.0,
      imagenUrl: imagenUrl,
      cantidadDisponible: cantidadDisponible,
      stockMinimo: (productoJson['stockMinimo'] as num?)?.toInt() ?? 0,
    );
  }
}
