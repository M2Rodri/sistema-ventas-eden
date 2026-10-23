import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Guarda cifrado lo último que mostró el inicio, para pintarlo al instante mientras llegan los datos nuevos.
class InicioGuardado {
  InicioGuardado._();

  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _prefijo = 'inicio_';
  static const claves = <String>['resumen', 'semana', 'alertas'];

  /// Guarda sin esperar y sin molestar: si falla, simplemente no queda nada.
  static void guardar(String clave, Object json) {
    _storage
        .write(key: '$_prefijo$clave', value: jsonEncode(json))
        .catchError((_) {});
  }

  /// Lo último guardado, o null si no hay nada (o no se pudo leer).
  static Future<Object?> leer(String clave) async {
    try {
      final crudo = await _storage.read(key: '$_prefijo$clave');
      return crudo == null ? null : jsonDecode(crudo) as Object;
    } catch (_) {
      return null;
    }
  }

  static Future<void> borrarTodo() async {
    for (final clave in claves) {
      try {
        await _storage.delete(key: '$_prefijo$clave');
      } catch (_) {}
    }
  }
}
