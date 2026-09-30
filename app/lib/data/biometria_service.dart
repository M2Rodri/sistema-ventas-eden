import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

/// Huella digital o rostro para desbloquear la app.
///
/// Es un candado extra sobre la sesión que ya queda guardada: no reemplaza la
/// contraseña. Si está activado, la app pide la huella al abrirse y al volver
/// después de un rato fuera.
class BiometriaService {
  BiometriaService();

  static const _clave = 'biometria_activada';

  /// Si el candado está activado. Se lee una vez al arrancar la app y se
  /// mantiene al día acá, para poder bloquear al instante, sin esperar al
  /// almacenamiento, cuando la app vuelve de segundo plano.
  static final ValueNotifier<bool> activa = ValueNotifier<bool>(false);

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  final LocalAuthentication _auth = LocalAuthentication();

  Future<void> cargar() async {
    activa.value = await _storage.read(key: _clave) == 'true';
  }

  Future<void> activar() async {
    await _storage.write(key: _clave, value: 'true');
    activa.value = true;
  }

  Future<void> desactivar() async {
    await _storage.delete(key: _clave);
    activa.value = false;
  }

  /// El celular tiene huella o rostro registrados.
  Future<bool> disponible() async {
    try {
      if (!await _auth.canCheckBiometrics) return false;
      return (await _auth.getAvailableBiometrics()).isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  /// Pide la huella o el rostro. Solo biometría: no se acepta el PIN del
  /// celular, para que el candado no se abra con algo que otros puedan saber.
  Future<bool> autenticar(String motivo) async {
    try {
      return await _auth.authenticate(
        localizedReason: motivo,
        biometricOnly: true,
        persistAcrossBackgrounding: true,
      );
    } catch (_) {
      return false;
    }
  }
}
