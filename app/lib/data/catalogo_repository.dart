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

  /// Mismo endpoint, con soloBajoMinimo=true: usado por Alertas de stock.
  Future<List<ProductoCatalogo>> obtenerBajoMinimo(String token) async {
    final json = await _apiClient.getList(
      '/api/v1/inventario/catalogo?soloBajoMinimo=true',
      token: token,
    );
    return json.map((item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>)).toList();
  }
}
