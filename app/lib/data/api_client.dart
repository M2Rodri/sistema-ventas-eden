import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import 'api_exception.dart';

/// Envoltorio chico sobre http: arma la URL completa, pone el header de
/// autorización cuando hay token, y traduce las respuestas de error del
/// backend a ApiException para que las pantallas no tengan que lidiar con
/// códigos HTTP ni parseo de JSON.
class ApiClient {
  const ApiClient();

  /// Qué hacer cuando una petición que llevaba sesión recibe 401 (token vencido
  /// o inválido). Lo define la app al arrancar (main.dart): borra la sesión y
  /// lleva al login. Un 403 no pasa por acá: es falta de permiso, no sesión vencida.
  static void Function()? alVencerSesion;

  static bool _sesionVencidaAvisada = false;

  /// Se llama al iniciar sesión: desde ahí, un nuevo 401 vuelve a contar.
  static void sesionIniciada() => _sesionVencidaAvisada = false;

  // ---------------------------------------------------------------------------
  // Caché de lecturas (GET). Vale para TODA la app: cualquier pantalla, actual o
  // futura, que lea con get / getList la aprovecha sin hacer nada.
  //  - Fresca (menos de [vidaFresca]): se reutiliza sin pedirla otra vez.
  //  - Algo vieja (hasta [vidaMaxima]): se muestra AL INSTANTE y se renueva por
  //    detrás, para que la próxima vez ya sea nueva. Nunca se espera al servidor.
  //  - Muy vieja o inexistente: se pide y se espera.
  //  - Las lecturas que se piden a la vez comparten una sola petición.
  //  - Cualquier escritura (POST, PUT, PATCH) vacía toda la caché, así que lo que
  //    el usuario guarda se ve de inmediato, y vuelve a llenarla por detrás.
  //  - La clave incluye el token: dos sesiones nunca comparten datos.
  //  - Solo se guardan respuestas correctas; un error nunca se queda en caché.
  // ---------------------------------------------------------------------------
  static Duration vidaFresca = const Duration(seconds: 60);
  static Duration vidaMaxima = const Duration(minutes: 3);
  static final Map<String, _EntradaCache> _cache = <String, _EntradaCache>{};
  static String? _ultimoToken;
  static Timer? _temporizadorPrecarga;

  static final StreamController<String> _actualizaciones =
      StreamController<String>.broadcast();

  /// Avisa (con la ruta pedida) cada vez que una renovación por detrás trajo datos
  /// DISTINTOS de los que ya se habían mostrado. Las pantallas abiertas se suscriben
  /// y se recargan solas, sin spinner, para corregirse en cuanto llega lo nuevo.
  static Stream<String> get actualizaciones => _actualizaciones.stream;

  /// Qué volver a pedir por detrás después de guardar algo (lo define la app al
  /// arrancar: PrecargaDatos.precargar).
  static Future<void> Function(String token)? precargar;

  /// Vacía la caché. Se usa al cerrar sesión y al tirar para refrescar una pantalla.
  static void vaciarCache() => _cache.clear();

  Future<http.Response> _leerConCache(String path, String token) {
    _ultimoToken = token;
    final clave = '$token|$path';
    final guardada = _cache[clave];
    if (guardada != null) {
      final edad = DateTime.now().difference(guardada.creada);
      if (edad < vidaFresca) return guardada.respuesta;
      if (edad < vidaMaxima && guardada.completada) {
        // Se entrega lo guardado ya mismo y se renueva por detrás.
        _renovar(clave, path, token, guardada);
        return guardada.respuesta;
      }
    }
    return _pedir(clave, path, token);
  }

  Future<http.Response> _pedir(String clave, String path, String token) {
    final respuesta = _enviar(
      () => http.get(_uri(path), headers: _headers(token)),
    );
    final entrada = _EntradaCache(DateTime.now(), respuesta);
    _cache[clave] = entrada;
    respuesta.then(
      (r) {
        if (r.statusCode >= 200 && r.statusCode < 300) {
          entrada.completada = true;
          entrada.cuerpo = r.body;
        } else if (identical(_cache[clave], entrada)) {
          _cache.remove(clave);
        }
      },
      onError: (Object _) {
        if (identical(_cache[clave], entrada)) _cache.remove(clave);
      },
    );
    return respuesta;
  }

  /// Pide otra vez una lectura que ya se entregó, sin que nadie la espere. Si
  /// sale bien reemplaza a la vieja; si falla, se queda la que había.
  void _renovar(String clave, String path, String token, _EntradaCache vieja) {
    if (vieja.renovando) return;
    vieja.renovando = true;
    final futura = _enviar(
      () => http.get(_uri(path), headers: _headers(token)),
    );
    futura.then((r) {
      if (r.statusCode >= 200 &&
          r.statusCode < 300 &&
          identical(_cache[clave], vieja)) {
        final cambio = r.body != vieja.cuerpo;
        final nueva =
            _EntradaCache(DateTime.now(), Future<http.Response>.value(r))
              ..completada = true
              ..cuerpo = r.body;
        _cache[clave] = nueva;
        if (cambio) _actualizaciones.add(path);
      } else {
        vieja.renovando = false;
      }
    }, onError: (Object _) => vieja.renovando = false);
  }

  /// Toda escritura pasa por acá: al terminar (salga bien o mal) la caché queda
  /// vacía y, pasado un momento, se vuelve a llenar por detrás.
  Future<http.Response> _escribir(
    String path,
    String? token,
    Future<http.Response> Function() accion,
  ) async {
    try {
      return await _enviar(accion);
    } finally {
      vaciarCache();
      final usar = token ?? _ultimoToken;
      if (usar != null && !path.endsWith('/auth/login') && precargar != null) {
        _temporizadorPrecarga?.cancel();
        _temporizadorPrecarga = Timer(
          const Duration(milliseconds: 800),
          () => precargar?.call(usar),
        );
      }
    }
  }

  Uri _uri(String path) => Uri.parse('${ApiConfig.baseUrl}$path');

  Map<String, String> _headers(String? token) => <String, String>{
    'Content-Type': 'application/json',
    if (token != null) 'Authorization': 'Bearer $token',
  };

  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> body, {
    String? token,
  }) async {
    final respuesta = await _escribir(
      path,
      token,
      () => http.post(
        _uri(path),
        headers: _headers(token),
        body: jsonEncode(body),
      ),
    );
    return _decodificar(respuesta, path, token);
  }

  Future<Map<String, dynamic>> put(
    String path,
    Map<String, dynamic> body, {
    required String token,
  }) async {
    final respuesta = await _escribir(
      path,
      token,
      () => http.put(
        _uri(path),
        headers: _headers(token),
        body: jsonEncode(body),
      ),
    );
    return _decodificar(respuesta, path, token);
  }

  /// PATCH sin body: alcanza para las acciones de estado que expone el
  /// backend (marcar entregado, cancelar), que no reciben nada más que el id
  /// en la ruta.
  Future<Map<String, dynamic>> patch(
    String path, {
    required String token,
  }) async {
    final respuesta = await _escribir(
      path,
      token,
      () => http.patch(_uri(path), headers: _headers(token)),
    );
    return _decodificar(respuesta, path, token);
  }

  /// PATCH con body, para las acciones que llevan datos (por ejemplo,
  /// completar la dirección, la transportadora y la guía de una venta).
  Future<Map<String, dynamic>> patchConCuerpo(
    String path,
    Map<String, dynamic> body, {
    required String token,
  }) async {
    final respuesta = await _escribir(
      path,
      token,
      () => http.patch(
        _uri(path),
        headers: _headers(token),
        body: jsonEncode(body),
      ),
    );
    return _decodificar(respuesta, path, token);
  }

  /// Sube un único archivo como multipart/form-data, en el campo "file" (el
  /// mismo nombre que espera POST /api/v1/pagos/{id}/comprobante).
  Future<Map<String, dynamic>> postArchivo(
    String path, {
    required File archivo,
    required String token,
  }) async {
    final respuesta = await _escribir(path, token, () async {
      final request = http.MultipartRequest('POST', _uri(path))
        ..headers['Authorization'] = 'Bearer $token'
        ..files.add(await http.MultipartFile.fromPath('file', archivo.path));
      final streamed = await request.send();
      return http.Response.fromStream(streamed);
    });
    return _decodificar(respuesta, path, token);
  }

  Future<Map<String, dynamic>> get(String path, {required String token}) async {
    final respuesta = await _leerConCache(path, token);
    return _decodificar(respuesta, path, token);
  }

  /// Igual que [get], pero para endpoints que devuelven un array JSON
  /// (`[...]`) en vez de un objeto (`{...}`), como los listados de
  /// productos e inventario.
  Future<List<dynamic>> getList(String path, {required String token}) async {
    final respuesta = await _leerConCache(path, token);
    return _decodificarLista(respuesta, path, token);
  }

  Future<http.Response> _enviar(Future<http.Response> Function() accion) async {
    try {
      return await accion().timeout(const Duration(seconds: 12));
    } on SocketException {
      throw const ApiException(
        'Sin conexión con el servidor',
        ApiErrorTipo.sinConexion,
      );
    } on TimeoutException {
      throw const ApiException(
        'Sin conexión con el servidor',
        ApiErrorTipo.sinConexion,
      );
    } on http.ClientException {
      throw const ApiException(
        'Sin conexión con el servidor',
        ApiErrorTipo.sinConexion,
      );
    }
  }

  Map<String, dynamic> _decodificar(
    http.Response respuesta,
    String path,
    String? token,
  ) {
    _verificarError(respuesta, path, token);
    if (respuesta.body.isEmpty) return <String, dynamic>{};
    return jsonDecode(respuesta.body) as Map<String, dynamic>;
  }

  List<dynamic> _decodificarLista(
    http.Response respuesta,
    String path,
    String? token,
  ) {
    _verificarError(respuesta, path, token);
    if (respuesta.body.isEmpty) return <dynamic>[];
    return jsonDecode(respuesta.body) as List<dynamic>;
  }

  /// Traduce un status HTTP de error a ApiException. No devuelve nada: si la
  /// respuesta está bien, simplemente vuelve y quien llamó sigue con el
  /// decodificado (de objeto o de lista) que corresponda.
  void _verificarError(http.Response respuesta, String path, String? token) {
    if (respuesta.statusCode == 401) {
      _avisarSiVencioLaSesion(path, token);
      throw ApiException(
        _extraerMensaje(respuesta.body) ?? 'Credenciales inválidas',
        ApiErrorTipo.credencialesInvalidas,
      );
    }
    if (respuesta.statusCode >= 500) {
      throw const ApiException(
        'El servidor tuvo un problema. Intentá de nuevo en un momento.',
        ApiErrorTipo.servidor,
      );
    }
    if (respuesta.statusCode >= 400) {
      throw ApiException(
        _extraerMensaje(respuesta.body) ?? 'No se pudo completar la operación',
        ApiErrorTipo.desconocido,
        codigo: extraerCodigoError(respuesta.body),
      );
    }
  }

  /// El 401 del login es "credenciales incorrectas", no una sesión vencida, y una
  /// petición sin token tampoco tenía sesión que perder. Se avisa una sola vez,
  /// aunque lleguen varios 401 juntos.
  void _avisarSiVencioLaSesion(String path, String? token) {
    final llevabaSesion = token != null;
    final esLogin = path.endsWith('/auth/login');
    if (!llevabaSesion || esLogin || _sesionVencidaAvisada) return;
    _sesionVencidaAvisada = true;
    alVencerSesion?.call();
  }

  String? _extraerMensaje(String cuerpo) => extraerMensajeError(cuerpo);
}

/// Código de negocio de un cuerpo de error ({"error": {"codigo": "..."}}), o
/// null si el cuerpo no lo trae.
String? extraerCodigoError(String cuerpo) {
  if (cuerpo.isEmpty) return null;
  try {
    final datos = jsonDecode(cuerpo);
    if (datos is Map<String, dynamic>) {
      final error = datos['error'];
      if (error is Map<String, dynamic>) return error['codigo'] as String?;
    }
  } on FormatException {
    // cuerpo que no es JSON
  }
  return null;
}

/// Mensaje de un cuerpo de error de la API. Formato único:
/// {"error": {"codigo": "...", "mensaje": "...", "campos": {...}}}. Mientras el
/// backend publicado siga con el formato anterior ({"error": "texto"}) también
/// se lee ese, igual que el {"message": "..."} de Spring y el texto plano.
String? extraerMensajeError(String cuerpo) {
  if (cuerpo.isEmpty) return null;
  try {
    final datos = jsonDecode(cuerpo);
    if (datos is Map<String, dynamic>) {
      final error = datos['error'];
      if (error is Map<String, dynamic>) return error['mensaje'] as String?;
      return error as String? ?? datos['message'] as String?;
    }
    return cuerpo;
  } on FormatException {
    return cuerpo;
  }
}

/// Una lectura guardada: la respuesta (o la petición todavía en camino) y cuándo se pidió.
class _EntradaCache {
  _EntradaCache(this.creada, this.respuesta);

  final DateTime creada;
  final Future<http.Response> respuesta;

  /// Ya llegó bien: se puede entregar aunque esté algo vieja.
  bool completada = false;

  /// El texto recibido, para saber si una renovación trajo algo distinto.
  String? cuerpo;

  /// Hay una renovación en camino: no se lanza otra.
  bool renovando = false;
}
