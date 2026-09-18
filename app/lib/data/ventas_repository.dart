import '../models/venta.dart';
import 'api_client.dart';

/// Capa de datos para Ventas y entregas.
///
///   GET /api/ventas          -> lista (VentaResponse[])
///   GET /api/ventas/{id}     -> detalle, mismo shape
///   POST /api/pagos          -> registrar un pago sobre el saldo pendiente
///   PATCH /api/ventas/{id}/entregar -> marcar entregada (rechaza en el
///     propio backend si queda saldo pendiente)
class VentasRepository {
  VentasRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<Venta>> obtenerVentas(String token) async {
    final json = await _apiClient.getList('/api/ventas', token: token);
    return json.map((item) => Venta.desdeApi(item as Map<String, dynamic>)).toList();
  }

  Future<Venta> obtenerVenta(int id, String token) async {
    final json = await _apiClient.get('/api/ventas/$id', token: token);
    return Venta.desdeApi(json);
  }

  Future<Venta> registrarPago({
    required int idVenta,
    required double monto,
    required MetodoPago metodoPago,
    String? referencia,
    required String token,
  }) async {
    await _apiClient.post(
      '/api/pagos',
      <String, dynamic>{
        'idVenta': idVenta,
        'monto': monto,
        'metodoPago': metodoPago.valorApi,
        if (referencia != null && referencia.isNotEmpty) 'referencia': referencia,
      },
      token: token,
    );
    // El pago no devuelve la venta actualizada, solo el pago: se vuelve a
    // pedir la venta para tener el saldoPendiente y el estado al día.
    return obtenerVenta(idVenta, token);
  }

  Future<Venta> marcarEntregado(int idVenta, String token) async {
    final json = await _apiClient.patch('/api/ventas/$idVenta/entregar', token: token);
    return Venta.desdeApi(json);
  }
}
