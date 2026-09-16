import '../models/dashboard_resumen.dart';
import 'api_client.dart';

class DashboardRepository {
  DashboardRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<DashboardResumen> obtenerResumenDelDia(String token) async {
    final json = await _apiClient.get('/api/dashboard/estadisticas', token: token);
    return DashboardResumen.fromJson(json);
  }
}
