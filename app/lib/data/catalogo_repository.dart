import '../models/producto_catalogo.dart';
import 'api_client.dart';
import 'inicio_guardado.dart';

/// Capa de datos para el catálogo de consulta (precio y stock).
class CatalogoRepository {
  CatalogoRepository({ApiClient? apiClient})
    : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<ProductoCatalogo>> obtenerCatalogo(String token) async {
    final json = await _apiClient.getList(
      '/api/v1/inventario/catalogo',
      token: token,
    );
    InicioGuardado.guardar('alertas', json);
    return json
        .map((item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>))
        .toList();
  }

  /// Los productos con stock bajo de la última vez (para mostrarlos al instante al abrir), o null.
  Future<List<ProductoCatalogo>?> alertasGuardadas() async {
    final json = await InicioGuardado.leer('alertas');
    if (json is! List) return null;
    try {
      return json
          .map(
            (item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>),
          )
          .toList();
    } catch (_) {
      return null;
    }
  }

  /// Solo ADMIN.
  Future<void> actualizarPrecioVenta(
    int idProducto,
    double nuevoPrecio,
    String token,
  ) async {
    final actual = await _apiClient.get(
      '/api/v1/productos/$idProducto',
      token: token,
    );
    const campos = <String>[
      'sku',
      'nombre',
      'descripcion',
      'modelo',
      'marca',
      'firmeza',
      'materialNucleo',
      'color',
      'materialArmazon',
      'idCategoria',
      'calidad',
      'precioCompra',
      'dimensiones',
      'stockMinimo',
      'tipoProducto',
      'activo',
    ];
    final cuerpo = <String, dynamic>{
      for (final campo in campos)
        if (actual[campo] != null) campo: actual[campo],
      'precioVenta': nuevoPrecio,
    };
    await _apiClient.put('/api/v1/productos/$idProducto', cuerpo, token: token);
  }

  /// Mismo endpoint, con soloBajoMinimo=true: usado por Alertas de stock.
  Future<List<ProductoCatalogo>> obtenerBajoMinimo(String token) async {
    final json = await _apiClient.getList(
      '/api/v1/inventario/catalogo?soloBajoMinimo=true',
      token: token,
    );
    return json
        .map((item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>))
        .toList();
  }
}
