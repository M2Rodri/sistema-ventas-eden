import 'dart:convert';

/// Lectura del JWT sin llamar al backend.
Map<String, dynamic>? _payload(String token) {
  final partes = token.split('.');
  if (partes.length != 3) return null;
  try {
    final payloadNormalizado = base64Url.normalize(partes[1]);
    return jsonDecode(utf8.decode(base64Url.decode(payloadNormalizado)))
        as Map<String, dynamic>;
  } catch (_) {
    return null;
  }
}

/// Revisa el "exp" del JWT para saber si venció.
bool tokenExpirado(String token) {
  final payload = _payload(token);
  if (payload == null) return true;

  final exp = payload['exp'];
  if (exp is! int) {
    return false; // sin campo exp, no hay como saber: se deja pasar
  }

  final expiracion = DateTime.fromMillisecondsSinceEpoch(exp * 1000);
  return DateTime.now().isAfter(expiracion);
}

/// Segundos que le quedan al token, o null si no se puede leer su "exp". Negativo si ya venció.
int? segundosRestantes(String token) {
  final exp = _payload(token)?['exp'];
  if (exp is! int) return null;
  return exp - DateTime.now().millisecondsSinceEpoch ~/ 1000;
}

/// Cuándo empezó la sesión del token (el inicio de sesión; las renovaciones lo conservan).
DateTime? inicioDeSesion(String token) {
  final payload = _payload(token);
  if (payload == null) return null;

  final inicio = payload['inicio'];
  if (inicio is num) {
    return DateTime.fromMillisecondsSinceEpoch(inicio.toInt());
  }
  final emision = payload['iat'];
  if (emision is num) {
    return DateTime.fromMillisecondsSinceEpoch(emision.toInt() * 1000);
  }
  return null;
}
