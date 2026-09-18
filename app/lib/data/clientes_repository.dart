import '../models/cliente.dart';
import 'api_client.dart';

/// GET /api/clientes, para el selector de cliente de Nueva venta.
class ClientesRepository {
  ClientesRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<Cliente>> obtenerClientes(String token) async {
    final json = await _apiClient.getList('/api/clientes', token: token);
    return json.map((item) => Cliente.desdeApi(item as Map<String, dynamic>)).toList();
  }
}
