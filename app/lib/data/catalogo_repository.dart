import '../models/producto_catalogo.dart';
import 'api_client.dart';

/// Capa de datos para el catálogo de consulta (precio y stock).
///
/// No existe un endpoint que devuelva nombre + precio + stock juntos: hay
/// que pedir productos activos e inventario por separado y cruzarlos por
/// idProducto. Ver ProductoCatalogo.desdeApi.
class CatalogoRepository {
  CatalogoRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<ProductoCatalogo>> obtenerCatalogo(String token) async {
    final productosJson = await _apiClient.getList('/api/productos/activos', token: token);
    final inventarioJson = await _apiClient.getList('/api/inventario', token: token);

    final stockPorProducto = <int, int>{};
    for (final item in inventarioJson) {
      final mapa = item as Map<String, dynamic>;
      final idProducto = mapa['idProducto'] as int?;
      if (idProducto == null) continue;
      stockPorProducto[idProducto] = (mapa['cantidadDisponible'] as num?)?.toInt() ?? 0;
    }

    return productosJson.map((item) {
      final mapa = item as Map<String, dynamic>;
      final id = mapa['id'] as int;
      return ProductoCatalogo.desdeApi(mapa, cantidadDisponible: stockPorProducto[id] ?? 0);
    }).toList();
  }
}
