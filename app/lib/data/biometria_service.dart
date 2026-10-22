import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

/// Candado de la app: huella, rostro o, si no responden, el bloqueo de pantalla
/// del celular (PIN, patrón o contraseña).
///
/// Es un candado extra sobre la sesión que ya queda guardada: no reemplaza la
/// contraseña. Si está activado, la app lo pide al abrirse y al volver
/// después de un rato fuera.
///
/// La preferencia guardada tiene tres estados: sin decidir (null), activado
/// ('true') y apagado a propósito por el usuario ('false'). Mientras no la haya
/// apagado, el candado se activa solo al iniciar sesión si el celular lo permite.
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

  /// La preferencia guardada: true (activado), false (apagado a propósito) o
  /// null (todavía sin decidir).
  Future<bool?> preferencia() async {
    final valor = await _storage.read(key: _clave);
    if (valor == null) return null;
    return valor == 'true';
  }

  Future<void> activar() async {
    await _storage.write(key: _clave, value: 'true');
    activa.value = true;
  }

  /// El usuario apagó el candado a propósito: se recuerda para no volver a
  /// activarlo solo al iniciar sesión.
  Future<void> apagar() async {
    await _storage.write(key: _clave, value: 'false');
    activa.value = false;
  }

  /// Al cerrar sesión no hay nada que proteger: se desactiva y queda "sin
  /// decidir", salvo que el usuario lo hubiera apagado a propósito.
  Future<void> desactivar() async {
    if (await _storage.read(key: _clave) == 'true') {
      await _storage.delete(key: _clave);
    }
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

  /// El celular tiene algún candado que usar: huella, rostro o bloqueo de
  /// pantalla (PIN, patrón, contraseña). Sin ninguno, cualquiera que lo tome
  /// entra a la app.
  Future<bool> candadoDisponible() async {
    try {
      return await _auth.isDeviceSupported();
    } catch (_) {
      return false;
    }
  }

  /// Pide la huella o el rostro y, si no responden (humedad, sensor), deja
  /// usar el PIN o el patrón del celular: son los que ya desbloquean el
  /// teléfono, así que no abren nada nuevo.
  Future<bool> autenticar(String motivo) async {
    try {
      return await _auth.authenticate(
        localizedReason: motivo,
        biometricOnly: false,
        persistAcrossBackgrounding: true,
      );
    } catch (_) {
      return false;
    }
  }
}
