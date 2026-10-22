import '../models/sesion.dart';
import 'api_client.dart';
import 'api_exception.dart';
import 'inicio_guardado.dart';
import 'biometria_service.dart';
import 'jwt_utils.dart';
import 'token_storage.dart';

/// Capa de datos para login/sesión. Las pantallas hablan con esto, nunca
/// directo con ApiClient o TokenStorage.
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

  /// Perfiles de sesión que entiende el backend. Con candado la sesión puede
  /// durar más (12 horas); sin ninguno, menos (4 horas).
  static const perfilConCandado = 'MOVIL_CON_CANDADO';
  static const perfilSinCandado = 'MOVIL_SIN_CANDADO';

  /// Sin candado, la sesión de la app no pasa de este tiempo desde que se inició.
  static const topeSinCandado = Duration(hours: 4);

  /// La sesión tendrá candado si el celular lo permite y el usuario no lo apagó
  /// a propósito.
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

    // El candado se activa solo la primera vez, salvo que el usuario lo haya
    // apagado a propósito: así ningún celular queda sin protección por descuido.
    if (conCandado && await _biometria.preferencia() == null) {
      await _biometria.activar();
    }
    return sesion;
  }

  /// Confirma la contraseña del usuario de la sesión guardada, sin cerrar la
  /// sesión ni cambiar de pantalla: sirve para desbloquear el candado cuando la
  /// huella y el bloqueo del celular no responden. Lanza ApiException si la
  /// contraseña es incorrecta (o si hay demasiados intentos).
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

  /// Sesión guardada, solo si existe, el token todavía no venció y, si el
  /// celular no tiene candado, no pasó el tope de ese caso. Si no sirve, la
  /// borra: así la próxima vez que se llame no hay que volver a chequear la
  /// fecha, ya no hay nada guardado.
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

  /// Si el candado está apagado (o el celular no tiene ninguno), la sesión no
  /// puede durar más que [topeSinCandado] desde que se inició, aunque el
  /// token pidiera más: por ejemplo, si el usuario apagó el candado después.
  Future<bool> _pasoElTopeSinCandado(Sesion sesion) async {
    await _biometria.cargar();
    final candadoActivo =
        BiometriaService.activa.value && await _biometria.candadoDisponible();
    if (candadoActivo) return false;

    final inicio = inicioDeSesion(sesion.token);
    if (inicio == null) return false;
    return DateTime.now().difference(inicio) > topeSinCandado;
  }

  /// Sin sesión no hay nada que proteger: se borra también el candado de
  /// huella, y quien inicie sesión después lo activa de nuevo si lo quiere.
  Future<void> cerrarSesion() async {
    ApiClient.vaciarCache();
    ApiClient.olvidarSesion();
    await InicioGuardado.borrarTodo();
    await _storage.borrarSesion();
    await _biometria.desactivar();
  }
}
