import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';
import 'package:local_auth_android/local_auth_android.dart';

/// Candado de la app con la huella o el rostro. Viene apagado: solo se activa desde Ajustes.
class BiometriaService {
  BiometriaService();

  /// Si es true, además de la huella se acepta el PIN o patrón del celular. Está apagado a propósito: la app no los ofrece.
  static const bool permitirBloqueoDelCelular = false;

  static const _clave = 'biometria_activada';

  /// Si el candado está activado.
  static final ValueNotifier<bool> activa = ValueNotifier<bool>(false);

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  final LocalAuthentication _auth = LocalAuthentication();

  Future<void> cargar() async {
    activa.value = await _storage.read(key: _clave) == 'true';
  }

  /// La preferencia guardada: true (activado), false (apagado desde Ajustes) o null (nunca se activó, equivale a apagado).
  Future<bool?> preferencia() async {
    final valor = await _storage.read(key: _clave);
    if (valor == null) return null;
    return valor == 'true';
  }

  Future<void> activar() async {
    await _storage.write(key: _clave, value: 'true');
    activa.value = true;
  }

  /// El usuario apagó el candado desde Ajustes.
  Future<void> apagar() async {
    await _storage.write(key: _clave, value: 'false');
    activa.value = false;
  }

  /// Al cerrar sesión no hay nada que proteger.
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

  /// El celular tiene con qué proteger la app: una huella o rostro registrado (o, si se permite, el bloqueo de pantalla).
  Future<bool> candadoDisponible() async {
    try {
      if (permitirBloqueoDelCelular) return await _auth.isDeviceSupported();
      return await _auth.canCheckBiometrics && (await _auth.getAvailableBiometrics()).isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  /// Pide la huella o el rostro; el diálogo de Android trae el botón "Cancelar".
  Future<bool> autenticar(String motivo) async {
    try {
      return await _auth.authenticate(
        localizedReason: motivo,
        authMessages: const <AuthMessages>[
          AndroidAuthMessages(
            signInTitle: 'Candado de la app',
            signInHint: 'Toca el sensor de huellas',
            cancelButton: 'Cancelar',
          ),
        ],
        biometricOnly: !permitirBloqueoDelCelular,
        persistAcrossBackgrounding: true,
      );
    } catch (_) {
      return false;
    }
  }
}
