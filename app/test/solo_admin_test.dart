import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:muebleria_eden_app/data/api_exception.dart';
import 'package:muebleria_eden_app/data/auth_repository.dart';
import 'package:muebleria_eden_app/data/biometria_service.dart';
import 'package:muebleria_eden_app/data/token_storage.dart';
import 'package:muebleria_eden_app/models/sesion.dart';
import 'package:muebleria_eden_app/models/usuario.dart';

/// La app móvil es solo para administradores: el Empleado no inicia sesión ni conserva una sesión guardada.
class _AlmacenEnMemoria extends TokenStorage {
  Sesion? guardada;
  bool borrada = false;

  @override
  Future<void> guardarSesion(Sesion sesion) async => guardada = sesion;

  @override
  Future<Sesion?> leerSesion() async => guardada;

  @override
  Future<void> borrarSesion() async {
    guardada = null;
    borrada = true;
  }
}

class _SinCandado extends BiometriaService {
  @override
  Future<bool> candadoDisponible() async => false;

  @override
  Future<bool?> preferencia() async => null;

  @override
  Future<void> desactivar() async {}

  @override
  Future<void> cargar() async {}
}

String _token() {
  String parte(Map<String, dynamic> datos) =>
      base64Url.encode(utf8.encode(jsonEncode(datos))).replaceAll('=', '');
  final exp =
      DateTime.now().add(const Duration(hours: 3)).millisecondsSinceEpoch ~/
      1000;
  return '${parte(<String, dynamic>{'alg': 'HS256'})}.${parte(<String, dynamic>{'exp': exp})}.firma';
}

void main() {
  late _AlmacenEnMemoria almacen;
  late AuthRepository repositorio;

  setUp(() {
    almacen = _AlmacenEnMemoria();
    repositorio = AuthRepository(storage: almacen, biometria: _SinCandado());
  });

  Future<T> respondiendo<T>(String rol, Future<T> Function() accion) {
    final cuerpo = jsonEncode(<String, dynamic>{
      'token': _token(),
      'id': 4,
      'nombre': 'Ana',
      'apellido': 'Pérez',
      'usuario': 'ana',
      'role': rol,
    });
    return http.runWithClient(
      accion,
      () => MockClient((_) async => http.Response(cuerpo, 200)),
    );
  }

  test('un administrador inicia sesión y queda guardada', () async {
    final sesion = await respondiendo(
      'ADMIN',
      () => repositorio.iniciarSesion(usuario: 'ana', password: 'x'),
    );

    expect(sesion.usuario.role, 'ADMIN');
    expect(almacen.guardada, isNotNull);
  });

  test('un empleado no inicia sesión y no queda nada guardado', () async {
    await respondiendo('EMPLEADO', () async {
      await expectLater(
        repositorio.iniciarSesion(usuario: 'luis', password: 'x'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.codigo, 'codigo', 'APP_SOLO_ADMIN')
              .having(
                (e) => e.mensaje,
                'mensaje',
                AuthRepository.mensajeSoloAdmin,
              ),
        ),
      );
    });

    expect(almacen.guardada, isNull);
  });

  test('si el servidor lo rechaza con 403 se muestra su mensaje', () async {
    final cuerpo = jsonEncode(<String, dynamic>{
      'error': <String, dynamic>{
        'codigo': 'APP_SOLO_ADMIN',
        'mensaje': AuthRepository.mensajeSoloAdmin,
      },
    });

    await http.runWithClient(() async {
      await expectLater(
        repositorio.iniciarSesion(usuario: 'luis', password: 'x'),
        throwsA(
          isA<ApiException>().having(
            (e) => e.mensaje,
            'mensaje',
            AuthRepository.mensajeSoloAdmin,
          ),
        ),
      );
    }, () => MockClient((_) async => http.Response(cuerpo, 403)));
  });

  Sesion sesionGuardada(String rol) => Sesion(
    token: _token(),
    usuario: Usuario(
      id: 4,
      nombre: 'Ana',
      apellido: 'Pérez',
      usuario: 'ana',
      role: rol,
    ),
  );

  test('una sesión guardada de empleado se descarta al abrir la app', () async {
    almacen.guardada = sesionGuardada('EMPLEADO');

    expect(await repositorio.sesionValidaGuardada(), isNull);
    expect(almacen.borrada, isTrue);
  });

  test('una sesión guardada de administrador se conserva', () async {
    almacen.guardada = sesionGuardada('ADMIN');

    expect(await repositorio.sesionValidaGuardada(), isNotNull);
  });
}
