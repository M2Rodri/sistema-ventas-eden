import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:muebleria_eden_app/data/api_client.dart';
import 'package:muebleria_eden_app/data/api_exception.dart';

/// La caché de lecturas de ApiClient: reutiliza lecturas recientes, comparte las
/// que van a la vez, se vacía al guardar algo y nunca mezcla sesiones ni guarda errores.
void main() {
  late List<String> pedidos;
  late int estadoRespuesta;

  setUp(() {
    pedidos = <String>[];
    estadoRespuesta = 200;
    ApiClient.vaciarCache();
    ApiClient.vidaFresca = const Duration(seconds: 60);
    ApiClient.vidaMaxima = const Duration(minutes: 3);
    ApiClient.precargar = null;
    ApiClient.alVencerSesion = null;
    ApiClient.sesionIniciada();
  });

  Future<T> conRed<T>(Future<T> Function() accion) {
    return http.runWithClient(
      accion,
      () => MockClient((peticion) async {
        pedidos.add('${peticion.method} ${peticion.url.path}');
        final cuerpo = peticion.method == 'GET' ? '[{"n":${pedidos.length}}]' : '{}';
        return http.Response(cuerpo, estadoRespuesta);
      }),
    );
  }

  test('una lectura reciente no se pide otra vez', () async {
    await conRed(() async {
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
    });
    expect(pedidos.length, 1);
  });

  test('lecturas simultáneas comparten una sola petición', () async {
    await conRed(() async {
      await Future.wait(<Future<Object>>[
        const ApiClient().getList('/api/v1/ventas', token: 'a'),
        const ApiClient().getList('/api/v1/ventas', token: 'a'),
        const ApiClient().getList('/api/v1/ventas', token: 'a'),
      ]);
    });
    expect(pedidos.length, 1);
  });

  test('cada lectura devuelve su propia copia (modificar una no afecta a la otra)', () async {
    late List<dynamic> primera;
    late List<dynamic> segunda;
    await conRed(() async {
      primera = await const ApiClient().getList('/api/v1/ventas', token: 'a');
      primera.add('basura');
      segunda = await const ApiClient().getList('/api/v1/ventas', token: 'a');
    });
    expect(segunda.length, 1);
  });

  test('una escritura vacía la caché: la siguiente lectura va al servidor', () async {
    await conRed(() async {
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await const ApiClient().patch('/api/v1/ventas/1/entregar', token: 'a');
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
    });
    expect(pedidos, <String>['GET /api/v1/ventas', 'PATCH /api/v1/ventas/1/entregar', 'GET /api/v1/ventas']);
  });

  test('dos sesiones nunca comparten datos', () async {
    await conRed(() async {
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await const ApiClient().getList('/api/v1/ventas', token: 'b');
    });
    expect(pedidos.length, 2);
  });

  test('un error no se guarda en la caché', () async {
    estadoRespuesta = 500;
    await conRed(() async {
      await expectLater(const ApiClient().getList('/api/v1/ventas', token: 'a'), throwsA(isA<ApiException>()));
      estadoRespuesta = 200;
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
    });
    expect(pedidos.length, 2);
  });

  test('después de guardar algo se vuelve a llenar por detrás con el token de la sesión', () async {
    final precargas = <String>[];
    ApiClient.precargar = (token) async => precargas.add(token);
    await conRed(() async {
      await const ApiClient().patch('/api/v1/ventas/1/entregar', token: 'a');
      await Future<void>.delayed(const Duration(milliseconds: 1000));
    });
    expect(precargas, <String>['a']);
  });

  test('el login no dispara la precarga', () async {
    final precargas = <String>[];
    ApiClient.precargar = (token) async => precargas.add(token);
    await conRed(() async {
      await const ApiClient().post('/api/v1/auth/login', <String, dynamic>{'usuario': 'x'});
      await Future<void>.delayed(const Duration(milliseconds: 1000));
    });
    expect(precargas, isEmpty);
  });

  test('una lectura algo vieja se entrega al instante y se renueva por detrás', () async {
    ApiClient.vidaFresca = Duration.zero; // todo cuenta como "algo viejo"
    late List<dynamic> primera;
    late List<dynamic> segunda;
    late List<dynamic> tercera;
    await conRed(() async {
      primera = await const ApiClient().getList('/api/v1/ventas', token: 'a');
      segunda = await const ApiClient().getList('/api/v1/ventas', token: 'a'); // vieja: sale ya
      await Future<void>.delayed(const Duration(milliseconds: 200)); // la renovación termina
      tercera = await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await Future<void>.delayed(const Duration(milliseconds: 100)); // se registra la renovación de la tercera
    });
    expect(segunda.first['n'], primera.first['n']); // la segunda fue la guardada
    expect(tercera.first['n'], 2); // la tercera ya trae lo renovado
    expect(pedidos.where((p) => p.startsWith('GET')).length, 3);
  });

  test('lo que se guarda se ve al instante: una renovación en camino no revive datos viejos', () async {
    ApiClient.vidaFresca = Duration.zero;
    final renovacion = Completer<void>();
    var lecturas = 0;
    await http.runWithClient(() async {
      await const ApiClient().getList('/api/v1/ventas', token: 'a'); // lectura 1
      await const ApiClient().getList('/api/v1/ventas', token: 'a'); // sale la vieja; renueva (lectura 2, frenada)
      await const ApiClient().patch('/api/v1/ventas/1/entregar', token: 'a'); // guarda: vacía la caché
      renovacion.complete(); // ahora la renovación vieja termina
      await Future<void>.delayed(const Duration(milliseconds: 100));
      await const ApiClient().getList('/api/v1/ventas', token: 'a'); // debe ir al servidor
    }, () => MockClient((peticion) async {
      if (peticion.method == 'GET') {
        lecturas++;
        if (lecturas == 2) await renovacion.future;
        return http.Response('[{"n":$lecturas}]', 200);
      }
      return http.Response('{}', 200);
    }));
    expect(lecturas, 3);
  });

  test('una renovación con datos distintos avisa a las pantallas abiertas', () async {
    ApiClient.vidaFresca = Duration.zero;
    final avisos = <String>[];
    final suscripcion = ApiClient.actualizaciones.listen(avisos.add);
    await conRed(() async {
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await const ApiClient().getList('/api/v1/ventas', token: 'a'); // vieja: sale ya y renueva
      await Future<void>.delayed(const Duration(milliseconds: 150));
    });
    await suscripcion.cancel();
    expect(avisos, <String>['/api/v1/ventas']);
  });

  test('una renovación con los mismos datos no avisa (no hay nada que corregir)', () async {
    ApiClient.vidaFresca = Duration.zero;
    final avisos = <String>[];
    final suscripcion = ApiClient.actualizaciones.listen(avisos.add);
    await http.runWithClient(() async {
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await const ApiClient().getList('/api/v1/ventas', token: 'a');
      await Future<void>.delayed(const Duration(milliseconds: 150));
    }, () => MockClient((_) async => http.Response('[{"n":1}]', 200)));
    await suscripcion.cancel();
    expect(avisos, isEmpty);
  });
}
