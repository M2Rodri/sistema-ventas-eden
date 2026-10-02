import '../models/dashboard_resumen.dart';
import '../models/ventas_semanal.dart';
import 'api_client.dart';

class DashboardRepository {
  DashboardRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<DashboardResumen> obtenerResumenDelDia(String token) async {
    final json = await _apiClient.get('/api/v1/dashboard/estadisticas', token: token);
    return DashboardResumen.fromJson(json);
  }

  /// Semana calendario (lunes a domingo) que contiene a [fecha]; sin fecha,
  /// la semana en curso.
  Future<VentasSemanal> obtenerVentasSemanal(String token, {DateTime? fecha}) async {
    final query = fecha == null
        ? ''
        : '?fecha=${fecha.year.toString().padLeft(4, '0')}-${fecha.month.toString().padLeft(2, '0')}-${fecha.day.toString().padLeft(2, '0')}';
    final json = await _apiClient.get('/api/v1/dashboard/ventas-semanal$query', token: token);
    return VentasSemanal.fromJson(json);
  }
}
