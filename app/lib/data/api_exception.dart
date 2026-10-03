/// Los tres casos que le importan a la pantalla de login (y en general a
/// cualquier pantalla que llame a la API): credenciales rechazadas, sin
/// conexión, o algo del lado del servidor.
enum ApiErrorTipo { credencialesInvalidas, sinConexion, servidor, desconocido }

class ApiException implements Exception {
  const ApiException(this.mensaje, this.tipo, {this.codigo});

  final String mensaje;
  final ApiErrorTipo tipo;

  /// Código de negocio de la API (por ejemplo PRECIO_EXCEDE_CATALOGO), si el
  /// servidor lo mandó.
  final String? codigo;

  @override
  String toString() => mensaje;
}
