// Sale de GET /api/v1/clientes (ClienteResponse en el backend). Solo lo que
// necesita el selector de cliente de Nueva venta.
class Cliente {
  const Cliente({
    required this.id,
    required this.nombreCompleto,
    required this.telefono,
    this.nitCi,
  });

  final int id;
  final String nombreCompleto;
  final String? telefono;

  /// NIT o CI del cliente (campo `nitCi` del servidor).
  final String? nitCi;

  factory Cliente.desdeApi(Map<String, dynamic> json) {
    return Cliente(
      id: json['id'] as int,
      nombreCompleto: json['nombreCompleto'] as String? ?? '',
      telefono: json['telefono'] as String?,
      nitCi: json['nitCi'] as String?,
    );
  }
}
