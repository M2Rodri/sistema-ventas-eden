import 'dart:convert';

/// Revisa el "exp" del JWT para saber si venció, sin llamar al backend.
/// No valida la firma (para eso está el backend en cada petición real):
/// esto es solo para decidir si vale la pena entrar directo o mandar de
/// nuevo a la pantalla de login.
bool tokenExpirado(String token) {
  final partes = token.split('.');
  if (partes.length != 3) return true;

  try {
    final payloadNormalizado = base64Url.normalize(partes[1]);
    final payload = jsonDecode(utf8.decode(base64Url.decode(payloadNormalizado)))
        as Map<String, dynamic>;
    final exp = payload['exp'];
    if (exp is! int) return false; // sin campo exp, no hay como saber: se deja pasar

    final expiracion = DateTime.fromMillisecondsSinceEpoch(exp * 1000);
    return DateTime.now().isAfter(expiracion);
  } catch (_) {
    return true;
  }
}
