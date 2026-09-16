/// Usuario autenticado. Mismos campos que devuelve el backend en
/// POST /api/auth/login (AuthResponse): id, nombre, apellido, email, role.
class Usuario {
  const Usuario({
    required this.id,
    required this.nombre,
    required this.apellido,
    required this.email,
    required this.role,
  });

  final int id;
  final String nombre;
  final String apellido;
  final String email;
  final String role;

  String get nombreCompleto => '$nombre $apellido';

  factory Usuario.fromJson(Map<String, dynamic> json) {
    return Usuario(
      id: (json['id'] as num).toInt(),
      nombre: json['nombre'] as String? ?? '',
      apellido: json['apellido'] as String? ?? '',
      email: json['email'] as String? ?? '',
      role: json['role'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'nombre': nombre,
        'apellido': apellido,
        'email': email,
        'role': role,
      };
}
