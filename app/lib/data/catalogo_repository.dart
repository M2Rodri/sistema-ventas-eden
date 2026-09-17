import '../models/producto_catalogo.dart';
import 'api_client.dart';

/// Capa de datos para el catálogo de consulta (precio y stock).
///
/// GET /api/inventario/catalogo devuelve nombre, precio y stock ya cruzados
/// en el servidor: antes esto pedía /api/productos/activos y /api/inventario
/// por separado y los cruzaba acá.
class CatalogoRepository {
  CatalogoRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<ProductoCatalogo>> obtenerCatalogo(String token) async {
    final json = await _apiClient.getList('/api/inventario/catalogo', token: token);
    return json.map((item) => ProductoCatalogo.desdeApi(item as Map<String, dynamic>)).toList();
  }
}
