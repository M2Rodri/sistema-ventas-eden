import '../models/sesion.dart';
import 'api_client.dart';
import 'jwt_utils.dart';
import 'token_storage.dart';

/// Capa de datos para login/sesión. Las pantallas hablan con esto, nunca
/// directo con ApiClient o TokenStorage.
class AuthRepository {
  AuthRepository({ApiClient? apiClient, TokenStorage? storage})
      : _apiClient = apiClient ?? const ApiClient(),
        _storage = storage ?? TokenStorage();

  final ApiClient _apiClient;
  final TokenStorage _storage;

  Future<Sesion> iniciarSesion({
    required String usuario,
    required String password,
  }) async {
    final json = await _apiClient.post('/api/auth/login', <String, dynamic>{
      'usuario': usuario,
      'password': password,
    });
    final sesion = Sesion.fromLoginResponse(json);
    await _storage.guardarSesion(sesion);
    return sesion;
  }

  /// Sesión guardada, solo si existe y el token todavía no venció. Si
  /// venció, la borra: así la próxima vez que se llame no hay que volver a
  /// chequear la fecha, ya no hay nada guardado.
  Future<Sesion?> sesionValidaGuardada() async {
    final sesion = await _storage.leerSesion();
    if (sesion == null) return null;

    if (tokenExpirado(sesion.token)) {
      await _storage.borrarSesion();
      return null;
    }

    return sesion;
  }

  Future<void> cerrarSesion() => _storage.borrarSesion();
}
