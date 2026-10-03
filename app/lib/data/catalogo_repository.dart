import '../models/producto_catalogo.dart';
import 'api_client.dart';

/// Capa de datos para el catálogo de consulta (precio y stock).
///
/// GET /api/v1/inventario/catalogo devuelve nombre, precio y stock ya cruzados
/// en el servidor: antes esto pedía /api/v1/productos/activos y /api/v1/inventario
/// por separado y los cruzaba acá.
class CatalogoRepository {
  CatalogoRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<ProductoCatalogo>> obtenerCatalogo(String token) async {
    final json = await _apiClient.getList('/api/v1/inventario/catalogo', token: token);
    return json.map((item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>)).toList();
  }

  /// Solo ADMIN. Cambia el precio de venta de un producto con el endpoint de
  /// edición (PUT /api/v1/productos/{id}), que pide el producto completo: se
  /// lee tal como está y se reenvía igual, salvo el precio.
  Future<void> actualizarPrecioVenta(int idProducto, double nuevoPrecio, String token) async {
    final actual = await _apiClient.get('/api/v1/productos/$idProducto', token: token);
    const campos = <String>[
      'sku', 'nombre', 'descripcion', 'modelo', 'marca', 'firmeza', 'materialNucleo', 'color',
      'materialArmazon', 'idCategoria', 'calidad', 'precioCompra', 'dimensiones', 'stockMinimo',
      'tipoProducto', 'activo',
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
    return json.map((item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>)).toList();
  }
}
