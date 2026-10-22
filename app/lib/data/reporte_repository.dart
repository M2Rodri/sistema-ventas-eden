import '../models/ganancia_producto.dart';
import 'api_client.dart';

class ReporteRepository {
  ReporteRepository({ApiClient? apiClient})
    : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<DateTime?> obtenerFechaInicio(String token) async {
    final json = await _apiClient.get('/api/v1/reportes/fecha-inicio', token: token);
    final fecha = json['fecha'];
    return fecha is String ? DateTime.tryParse(fecha) : null;
  }

  Future<List<GananciaProducto>> obtenerGananciasPorProducto({
    required String token,
    required DateTime inicio,
    required DateTime fin,
  }) async {
    String iso(DateTime fecha) => fecha.toIso8601String();
    final json = await _apiClient.getList(
      '/api/v1/reportes/ganancias-por-producto?inicio=${Uri.encodeQueryComponent(iso(inicio))}&fin=${Uri.encodeQueryComponent(iso(fin))}',
      token: token,
    );
    return json
        .map((item) => GananciaProducto.fromJson(item as Map<String, dynamic>))
        .toList();
  }
}
