import '../models/sesion.dart';
import 'api_client.dart';
import 'api_exception.dart';
import 'inicio_guardado.dart';
import 'biometria_service.dart';
import 'jwt_utils.dart';
import 'token_storage.dart';

/// Capa de datos para login/sesión. Las pantallas hablan con esto, nunca directo con ApiClient o TokenStorage.
class AuthRepository {
  AuthRepository({
    ApiClient? apiClient,
    TokenStorage? storage,
    BiometriaService? biometria,
  }) : _apiClient = apiClient ?? const ApiClient(),
       _storage = storage ?? TokenStorage(),
       _biometria = biometria ?? BiometriaService();

  final ApiClient _apiClient;
  final TokenStorage _storage;
  final BiometriaService _biometria;

  /// Perfiles de sesión que entiende el backend.
  static const perfilConCandado = 'MOVIL_CON_CANDADO';
  static const perfilSinCandado = 'MOVIL_SIN_CANDADO';

  /// Sin candado, la sesión de la app no pasa de este tiempo desde que se inició.
  static const topeSinCandado = Duration(hours: 4);

  /// La sesión tendrá candado si el celular lo permite y el usuario no lo apagó a propósito.
  Future<bool> _habraCandado() async {
    if (!await _biometria.candadoDisponible()) return false;
    return await _biometria.preferencia() != false;
  }

  Future<Sesion> iniciarSesion({
    required String usuario,
    required String password,
  }) async {
    final conCandado = await _habraCandado();
    final json = await _apiClient.post('/api/v1/auth/login', <String, dynamic>{
      'usuario': usuario,
      'password': password,
      'perfilSesion': conCandado ? perfilConCandado : perfilSinCandado,
    });
    final sesion = Sesion.fromLoginResponse(json);
    await _storage.guardarSesion(sesion);
    ApiClient.nuevaSesion(sesion.token);

    // El candado se activa solo la primera vez, salvo que el usuario lo haya apagado a propósito.
    if (conCandado && await _biometria.preferencia() == null) {
      await _biometria.activar();
    }
    return sesion;
  }

  /// Confirma la contraseña del usuario de la sesión guardada, sin cerrar la sesión ni cambiar de pantalla.
  Future<void> reautenticar(String password) async {
    final guardada = await _storage.leerSesion();
    if (guardada == null) {
      throw const ApiException(
        'No hay una sesión guardada. Inicia sesión de nuevo.',
        ApiErrorTipo.credencialesInvalidas,
      );
    }
    final conCandado = await _habraCandado();
    final json = await _apiClient.post('/api/v1/auth/login', <String, dynamic>{
      'usuario': guardada.usuario.usuario,
      'password': password,
      'perfilSesion': conCandado ? perfilConCandado : perfilSinCandado,
    });
    final nueva = Sesion.fromLoginResponse(json);
    await _storage.guardarSesion(nueva);
    // Las pantallas siguen con el token que tenían: el cliente HTTP usa el nuevo.
    ApiClient.reemplazarToken(nueva.token);
  }

  /// Sesión guardada, si existe, el token no venció y no pasó el tope (sin candado).
  Future<Sesion?> sesionValidaGuardada() async {
    final sesion = await _storage.leerSesion();
    if (sesion == null) return null;

    if (tokenExpirado(sesion.token) || await _pasoElTopeSinCandado(sesion)) {
      await _storage.borrarSesion();
      await _biometria.desactivar();
      ApiClient.olvidarSesion();
      return null;
    }

    ApiClient.nuevaSesion(sesion.token);
    return sesion;
  }

  /// Sin candado, la sesión no puede durar más que [topeSinCandado] desde que se inició.
  Future<bool> _pasoElTopeSinCandado(Sesion sesion) async {
    await _biometria.cargar();
    final candadoActivo =
        BiometriaService.activa.value && await _biometria.candadoDisponible();
    if (candadoActivo) return false;

    final inicio = inicioDeSesion(sesion.token);
    if (inicio == null) return false;
    return DateTime.now().difference(inicio) > topeSinCandado;
  }

  /// Sin sesión se borra también el candado; quien inicie después lo activa de nuevo si quiere.
  Future<void> cerrarSesion() async {
    ApiClient.vaciarCache();
    ApiClient.olvidarSesion();
    await InicioGuardado.borrarTodo();
    await _storage.borrarSesion();
    await _biometria.desactivar();
  }
}
