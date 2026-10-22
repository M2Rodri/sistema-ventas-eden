import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:muebleria_eden_app/data/api_client.dart';
import 'package:muebleria_eden_app/data/jwt_utils.dart';

/// Un JWT sin firma real, solo con los datos que lee la app (exp, inicio, iat).
String _token({
  required Duration restante,
  DateTime? inicio,
  DateTime? emision,
  String marca = '',
}) {
  String codificar(Map<String, Object?> datos) =>
      base64Url.encode(utf8.encode(jsonEncode(datos))).replaceAll('=', '');

  final exp = DateTime.now().add(restante).millisecondsSinceEpoch ~/ 1000;
  return '${codificar(<String, Object?>{'alg': 'HS256'})}.'
      '${codificar(<String, Object?>{
        'sub': 'ana',
        'exp': exp,
        'marca': marca,
        if (inicio != null) 'inicio': inicio.millisecondsSinceEpoch,
        if (emision != null) 'iat': emision.millisecondsSinceEpoch ~/ 1000,
      })}.firma';
}

String _respuestaLogin(String token) => jsonEncode(<String, Object?>{
  'token': token,
  'tipo': 'Bearer',
  'id': 7,
  'nombre': 'Ana',
  'apellido': 'Pérez',
  'usuario': 'ana',
  'role': 'ADMIN',
});

void main() {
  group('lectura del token', () {
    test('segundosRestantes cuenta lo que falta para vencer', () {
      final segundos = segundosRestantes(_token(restante: const Duration(hours: 2)))!;
      expect(segundos, inInclusiveRange(2 * 3600 - 5, 2 * 3600));
    });

    test('segundosRestantes es negativo si ya venció y null si no se puede leer', () {
      expect(segundosRestantes(_token(restante: const Duration(minutes: -5)))!, lessThan(0));
      expect(segundosRestantes('no-es-un-token'), isNull);
    });

    test('inicioDeSesion usa el dato "inicio" y, si no está, la emisión', () {
      final inicio = DateTime(2026, 10, 22, 8, 0);
      final conInicio = _token(restante: const Duration(hours: 1), inicio: inicio);
      expect(inicioDeSesion(conInicio), inicio);

      final emision = DateTime(2026, 10, 22, 9, 30);
      final soloEmision = _token(restante: const Duration(hours: 1), emision: emision);
      expect(inicioDeSesion(soloEmision), emision);

      expect(inicioDeSesion('basura'), isNull);
    });
  });

  group('renovación automática', () {
    late List<http.Request> pedidos;

    setUp(() {
      pedidos = <http.Request>[];
      ApiClient.vaciarCache();
    });

    tearDown(ApiClient.olvidarSesion);

    Future<T> conServidor<T>(
      Future<T> Function() accion, {
      required http.Response Function(http.Request) responder,
    }) {
      return http.runWithClient(
        accion,
        () => MockClient((pedido) async {
          pedidos.add(pedido);
          return responder(pedido);
        }),
      );
    }

    test('con poco tiempo por delante renueva y usa el token nuevo', () async {
      final viejo = _token(restante: const Duration(minutes: 30), marca: 'viejo');
      final nuevo = _token(restante: const Duration(hours: 4), marca: 'nuevo');
      ApiClient.nuevaSesion(viejo);

      await conServidor(() async {
        await const ApiClient().get('/api/v1/ventas', token: viejo);
      }, responder: (pedido) {
        if (pedido.url.path.endsWith('/auth/renovar')) {
          return http.Response(_respuestaLogin(nuevo), 200);
        }
        return http.Response('{}', 200);
      });

      expect(pedidos.length, 2);
      expect(pedidos[0].url.path, '/api/v1/auth/renovar');
      expect(pedidos[0].headers['Authorization'], 'Bearer $viejo');
      expect(pedidos[1].url.path, '/api/v1/ventas');
      expect(pedidos[1].headers['Authorization'], 'Bearer $nuevo');
    });

    test('la pantalla sigue pasando el token viejo y el cliente usa el vigente', () async {
      final viejo = _token(restante: const Duration(minutes: 10), marca: 'viejo');
      final nuevo = _token(restante: const Duration(hours: 4), marca: 'nuevo');
      ApiClient.nuevaSesion(viejo);

      await conServidor(() async {
        await const ApiClient().get('/api/v1/ventas', token: viejo);
        // Segunda llamada: el token vigente ya tiene 4 horas, no hay otra renovación.
        await const ApiClient().get('/api/v1/clientes', token: viejo);
      }, responder: (pedido) {
        if (pedido.url.path.endsWith('/auth/renovar')) {
          return http.Response(_respuestaLogin(nuevo), 200);
        }
        return http.Response('{}', 200);
      });

      final renovaciones = pedidos.where((p) => p.url.path.endsWith('/auth/renovar'));
      expect(renovaciones.length, 1);
      expect(pedidos.last.url.path, '/api/v1/clientes');
      expect(pedidos.last.headers['Authorization'], 'Bearer $nuevo');
    });

    test('con tiempo de sobra no renueva', () async {
      final token = _token(restante: const Duration(hours: 3));
      ApiClient.nuevaSesion(token);

      await conServidor(() async {
        await const ApiClient().get('/api/v1/ventas', token: token);
      }, responder: (_) => http.Response('{}', 200));

      expect(pedidos.length, 1);
      expect(pedidos.single.url.path, '/api/v1/ventas');
    });

    test('si la renovación falla, la llamada sigue con el token que hay', () async {
      final viejo = _token(restante: const Duration(minutes: 20));
      ApiClient.nuevaSesion(viejo);

      await conServidor(() async {
        await const ApiClient().get('/api/v1/ventas', token: viejo);
      }, responder: (pedido) {
        if (pedido.url.path.endsWith('/auth/renovar')) {
          return http.Response('{"error":{"codigo":"X","mensaje":"m"}}', 401);
        }
        return http.Response('{}', 200);
      });

      expect(pedidos.last.url.path, '/api/v1/ventas');
      expect(pedidos.last.headers['Authorization'], 'Bearer $viejo');
    });

    test('varias llamadas juntas piden una sola renovación', () async {
      final viejo = _token(restante: const Duration(minutes: 5), marca: 'viejo');
      final nuevo = _token(restante: const Duration(hours: 4), marca: 'nuevo');
      ApiClient.nuevaSesion(viejo);

      await conServidor(() async {
        await Future.wait(<Future<void>>[
          const ApiClient().get('/api/v1/ventas', token: viejo),
          const ApiClient().get('/api/v1/clientes', token: viejo),
          const ApiClient().get('/api/v1/productos', token: viejo),
        ]);
      }, responder: (pedido) {
        if (pedido.url.path.endsWith('/auth/renovar')) {
          return http.Response(_respuestaLogin(nuevo), 200);
        }
        return http.Response('{}', 200);
      });

      final renovaciones = pedidos.where((p) => p.url.path.endsWith('/auth/renovar'));
      expect(renovaciones.length, 1);
    });

    test('sin sesión registrada no renueva nada', () async {
      final token = _token(restante: const Duration(minutes: 5));
      // No se llamó a nuevaSesion: por ejemplo, el login.

      await conServidor(() async {
        await const ApiClient().get('/api/v1/ventas', token: token);
      }, responder: (_) => http.Response('{}', 200));

      expect(pedidos.length, 1);
    });
  });
}
