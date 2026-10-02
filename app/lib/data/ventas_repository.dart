import 'dart:io';

import '../models/venta.dart';
import 'api_client.dart';

/// Capa de datos para Ventas y entregas.
///
///   GET /api/v1/ventas          -> lista (VentaResponse[])
///   GET /api/v1/ventas/{id}     -> detalle, mismo shape
///   POST /api/v1/pagos          -> registrar un pago sobre el saldo pendiente
///   PATCH /api/v1/ventas/{id}/entregar -> marcar entregada (rechaza en el
///     propio backend si queda saldo pendiente)
class VentasRepository {
  VentasRepository({ApiClient? apiClient}) : _apiClient = apiClient ?? const ApiClient();

  final ApiClient _apiClient;

  Future<List<Venta>> obtenerVentas(String token) async {
    final json = await _apiClient.getList('/api/v1/ventas', token: token);
    return json.map((item) => Venta.desdeApi(item as Map<String, dynamic>)).toList();
  }

  Future<Venta> obtenerVenta(int id, String token) async {
    final json = await _apiClient.get('/api/v1/ventas/$id', token: token);
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
      '/api/v1/pagos',
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
    final json = await _apiClient.patch('/api/v1/ventas/$idVenta/entregar', token: token);
    return Venta.desdeApi(json);
  }

  /// Solo ADMIN: corrige una entrega marcada por error, ENTREGADO -> PENDIENTE.
  Future<Venta> corregirAPendiente(int idVenta, String token) async {
    final json = await _apiClient.patch('/api/v1/ventas/$idVenta/deshacer-entrega', token: token);
    return Venta.desdeApi(json);
  }

  /// Solo ADMIN. Un valor vacío borra el dato. La transportadora y la guía
  /// solo se guardan en ventas por transportadora.
  Future<Venta> actualizarDatosEntrega(
    int idVenta, {
    String? direccionDestino,
    String? transportadora,
    String? guiaRemision,
    required String token,
  }) async {
    final json = await _apiClient.patchConCuerpo(
      '/api/v1/ventas/$idVenta/datos-entrega',
      <String, dynamic>{
        'direccionDestino': direccionDestino ?? '',
        'transportadora': transportadora ?? '',
        'guiaRemision': guiaRemision ?? '',
      },
      token: token,
    );
    return Venta.desdeApi(json);
  }

  Future<Venta> crearVenta(NuevaVentaRequest request, String token) async {
    final json = await _apiClient.post('/api/v1/ventas', request.toJson(), token: token);
    return Venta.desdeApi(json);
  }

  /// El comprobante viaja aparte, sobre un pago ya creado: si esto falla la
  /// venta ya quedó registrada igual (mismo comportamiento que el formulario
  /// web, que avisa el error pero no revierte nada).
  Future<void> adjuntarComprobante(int idPago, File archivo, String token) async {
    await _apiClient.postArchivo('/api/v1/pagos/$idPago/comprobante', archivo: archivo, token: token);
  }
}
