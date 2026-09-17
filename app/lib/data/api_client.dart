import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import 'api_exception.dart';

/// Envoltorio chico sobre http: arma la URL completa, pone el header de
/// autorización cuando hay token, y traduce las respuestas de error del
/// backend a ApiException para que las pantallas no tengan que lidiar con
/// códigos HTTP ni parseo de JSON.
class ApiClient {
  const ApiClient();

  Uri _uri(String path) => Uri.parse('${ApiConfig.baseUrl}$path');

  Map<String, String> _headers(String? token) => <String, String>{
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };

  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> body, {
    String? token,
  }) async {
    final respuesta = await _enviar(
      () => http.post(_uri(path), headers: _headers(token), body: jsonEncode(body)),
    );
    return _decodificar(respuesta);
  }

  Future<Map<String, dynamic>> get(String path, {required String token}) async {
    final respuesta = await _enviar(
      () => http.get(_uri(path), headers: _headers(token)),
    );
    return _decodificar(respuesta);
  }

  /// Igual que [get], pero para endpoints que devuelven un array JSON
  /// (`[...]`) en vez de un objeto (`{...}`), como los listados de
  /// productos e inventario.
  Future<List<dynamic>> getList(String path, {required String token}) async {
    final respuesta = await _enviar(
      () => http.get(_uri(path), headers: _headers(token)),
    );
    return _decodificarLista(respuesta);
  }

  Future<http.Response> _enviar(Future<http.Response> Function() accion) async {
    try {
      return await accion().timeout(const Duration(seconds: 12));
    } on SocketException {
      throw const ApiException('Sin conexión con el servidor', ApiErrorTipo.sinConexion);
    } on TimeoutException {
      throw const ApiException('Sin conexión con el servidor', ApiErrorTipo.sinConexion);
    } on http.ClientException {
      throw const ApiException('Sin conexión con el servidor', ApiErrorTipo.sinConexion);
    }
  }

  Map<String, dynamic> _decodificar(http.Response respuesta) {
    _verificarError(respuesta);
    if (respuesta.body.isEmpty) return <String, dynamic>{};
    return jsonDecode(respuesta.body) as Map<String, dynamic>;
  }

  List<dynamic> _decodificarLista(http.Response respuesta) {
    _verificarError(respuesta);
    if (respuesta.body.isEmpty) return <dynamic>[];
    return jsonDecode(respuesta.body) as List<dynamic>;
  }

  /// Traduce un status HTTP de error a ApiException. No devuelve nada: si la
  /// respuesta está bien, simplemente vuelve y quien llamó sigue con el
  /// decodificado (de objeto o de lista) que corresponda.
  void _verificarError(http.Response respuesta) {
    if (respuesta.statusCode == 401) {
      throw ApiException(
        _extraerMensaje(respuesta.body) ?? 'Credenciales inválidas',
        ApiErrorTipo.credencialesInvalidas,
      );
    }
    if (respuesta.statusCode >= 500) {
      throw const ApiException(
        'El servidor tuvo un problema. Intentá de nuevo en un momento.',
        ApiErrorTipo.servidor,
      );
    }
    if (respuesta.statusCode >= 400) {
      throw ApiException(
        _extraerMensaje(respuesta.body) ?? 'No se pudo completar la operación',
        ApiErrorTipo.desconocido,
      );
    }
  }

  /// El backend a veces manda el error como JSON ({"error": "..."} o el
  /// {"message": "..."} por defecto de Spring) y a veces como texto plano
  /// (el 401 de login manda solo "Credenciales inválidas", sin comillas ni
  /// llaves). Se intenta como JSON primero; si no es JSON válido, se usa el
  /// texto tal cual.
  String? _extraerMensaje(String cuerpo) {
    if (cuerpo.isEmpty) return null;
    try {
      final datos = jsonDecode(cuerpo);
      if (datos is Map<String, dynamic>) {
        return datos['error'] as String? ?? datos['message'] as String?;
      }
      return cuerpo;
    } on FormatException {
      return cuerpo;
    }
  }
}
