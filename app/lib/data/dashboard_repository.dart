import '../models/dashboard_resumen.dart';
import '../models/ventas_semanal.dart';
import 'api_client.dart';
import 'inicio_guardado.dart';

class DashboardRepository {
  DashboardRepository({ApiClient? apiClient})
    : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<DashboardResumen> obtenerResumenDelDia(String token) async {
    final json = await _apiClient.get(
      '/api/v1/dashboard/estadisticas',
      token: token,
    );
    InicioGuardado.guardar('resumen', json);
    return DashboardResumen.fromJson(json);
  }

  /// Semana calendario (lunes a domingo) que contiene a [fecha]; sin fecha,
  /// la semana en curso.
  Future<VentasSemanal> obtenerVentasSemanal(
    String token, {
    DateTime? fecha,
  }) async {
    final query = fecha == null
        ? ''
        : '?fecha=${fecha.year.toString().padLeft(4, '0')}-${fecha.month.toString().padLeft(2, '0')}-${fecha.day.toString().padLeft(2, '0')}';
    final json = await _apiClient.get(
      '/api/v1/dashboard/ventas-semanal$query',
      token: token,
    );
    if (fecha == null) InicioGuardado.guardar('semana', json);
    return VentasSemanal.fromJson(json);
  }

  /// Lo último que se vio en el inicio (para mostrarlo al instante al abrir), o null.
  Future<DashboardResumen?> resumenGuardado() async {
    final json = await InicioGuardado.leer('resumen');
    if (json is! Map<String, dynamic>) return null;
    try {
      return DashboardResumen.fromJson(json);
    } catch (_) {
      return null;
    }
  }

  Future<VentasSemanal?> semanaGuardada() async {
    final json = await InicioGuardado.leer('semana');
    if (json is! Map<String, dynamic>) return null;
    try {
      return VentasSemanal.fromJson(json);
    } catch (_) {
      return null;
    }
  }
}
