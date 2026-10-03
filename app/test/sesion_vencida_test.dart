import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:muebleria_eden_app/data/api_client.dart';
import 'package:muebleria_eden_app/data/api_exception.dart';

/// Una petición con sesión que recibe 401 avisa que la sesión venció; el login
/// rechazado, un 403 o una petición sin token no.
void main() {
  late int avisos;

  setUp(() {
    avisos = 0;
    ApiClient.alVencerSesion = () => avisos++;
    ApiClient.sesionIniciada();
  });

  tearDown(() => ApiClient.alVencerSesion = null);

  Future<T> conRespuesta<T>(int estado, Future<T> Function() accion) {
    return http.runWithClient(
      accion,
      () => MockClient((_) async => http.Response('{"error":{"codigo":"X","mensaje":"m"}}', estado)),
    );
  }

  test('un 401 con token avisa una vez y sigue lanzando el error', () async {
    await conRespuesta(401, () async {
      await expectLater(
        const ApiClient().get('/api/v1/ventas', token: 'abc'),
        throwsA(isA<ApiException>()),
      );
    });
    expect(avisos, 1);
  });

  test('varios 401 juntos avisan una sola vez, y de nuevo después de iniciar sesión', () async {
    await conRespuesta(401, () async {
      for (var i = 0; i < 3; i++) {
        await expectLater(const ApiClient().get('/api/v1/ventas', token: 'abc'), throwsA(isA<ApiException>()));
      }
    });
    expect(avisos, 1);

    ApiClient.sesionIniciada();
    await conRespuesta(401, () async {
      await expectLater(const ApiClient().get('/api/v1/ventas', token: 'abc'), throwsA(isA<ApiException>()));
    });
    expect(avisos, 2);
  });

  test('el 401 del login (credenciales incorrectas) no cierra nada', () async {
    await conRespuesta(401, () async {
      await expectLater(
        const ApiClient().post('/api/v1/auth/login', <String, dynamic>{'usuario': 'a', 'password': 'b'}),
        throwsA(isA<ApiException>()),
      );
    });
    expect(avisos, 0);
  });

  test('una petición sin token no tenía sesión que perder', () async {
    await conRespuesta(401, () async {
      await expectLater(
        const ApiClient().post('/api/v1/otra', <String, dynamic>{}),
        throwsA(isA<ApiException>()),
      );
    });
    expect(avisos, 0);
  });

  test('un 403 no cierra la sesión', () async {
    await conRespuesta(403, () async {
      await expectLater(
        const ApiClient().get('/api/v1/usuarios', token: 'abc'),
        throwsA(isA<ApiException>()),
      );
    });
    expect(avisos, 0);
  });
}
