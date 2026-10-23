import 'usuario.dart';

/// Token JWT + datos del usuario.
class Sesion {
  const Sesion({required this.token, required this.usuario});

  final String token;
  final Usuario usuario;

  /// El login devuelve token y datos del usuario en un solo objeto plano.
  factory Sesion.fromLoginResponse(Map<String, dynamic> json) {
    return Sesion(
      token: json['token'] as String,
      usuario: Usuario.fromJson(json),
    );
  }

  factory Sesion.fromJson(Map<String, dynamic> json) {
    return Sesion(
      token: json['token'] as String,
      usuario: Usuario.fromJson(json['usuario'] as Map<String, dynamic>),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
    'token': token,
    'usuario': usuario.toJson(),
  };
}
