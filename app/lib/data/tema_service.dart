import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Servicio para controlar y persistir la preferencia de tema (claro u oscuro).
class TemaService {
  TemaService();

  static const _clave = 'tema_oscuro_activado';

  /// Si el tema oscuro está activado. Por defecto es falso (tema claro).
  static final ValueNotifier<bool> modoOscuro = ValueNotifier<bool>(false);

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  /// Carga la preferencia guardada al iniciar la app.
  Future<void> cargar() async {
    try {
      final guardado = await _storage.read(key: _clave);
      modoOscuro.value = guardado == 'true';
    } catch (_) {
      modoOscuro.value = false;
    }
  }

  /// Activa o desactiva el tema oscuro y lo guarda en almacenamiento seguro.
  Future<void> alternar(bool activarOscuro) async {
    modoOscuro.value = activarOscuro;
    try {
      if (activarOscuro) {
        await _storage.write(key: _clave, value: 'true');
      } else {
        await _storage.delete(key: _clave);
      }
    } catch (_) {}
  }
}
