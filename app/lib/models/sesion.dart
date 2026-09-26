import 'usuario.dart';

/// Token JWT + datos del usuario. Lo que se guarda en flutter_secure_storage
/// y se manda en cada petición como header `Authorization: Bearer TOKEN`.
class Sesion {
  const Sesion({required this.token, required this.usuario});

  final String token;
  final Usuario usuario;

  /// POST /api/auth/login devuelve todo en un solo objeto plano
  /// (token, tipo, id, nombre, apellido, usuario, role), no anidado.
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
