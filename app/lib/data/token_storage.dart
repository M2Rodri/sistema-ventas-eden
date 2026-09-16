import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/sesion.dart';

/// Guarda la sesión (token + usuario) en Keychain (iOS) / EncryptedSharedPreferences
/// (Android), no en SharedPreferences: SharedPreferences guarda en texto
/// plano, cualquiera con acceso al teléfono (o un backup) podría leer el
/// token directo.
class TokenStorage {
  TokenStorage();

  static const _claveSesion = 'sesion';

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  Future<void> guardarSesion(Sesion sesion) async {
    await _storage.write(key: _claveSesion, value: jsonEncode(sesion.toJson()));
  }

  Future<Sesion?> leerSesion() async {
    final crudo = await _storage.read(key: _claveSesion);
    if (crudo == null) return null;

    try {
      return Sesion.fromJson(jsonDecode(crudo) as Map<String, dynamic>);
    } catch (_) {
      // Dato corrupto o de un formato viejo: mejor borrarlo que romper el
      // arranque de la app.
      await borrarSesion();
      return null;
    }
  }

  Future<void> borrarSesion() async {
    await _storage.delete(key: _claveSesion);
  }
}
