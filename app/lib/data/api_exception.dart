/// Los tres casos que le importan a la pantalla de login (y en general a
/// cualquier pantalla que llame a la API): credenciales rechazadas, sin
/// conexión, o algo del lado del servidor.
enum ApiErrorTipo { credencialesInvalidas, sinConexion, servidor, desconocido }

class ApiException implements Exception {
  const ApiException(this.mensaje, this.tipo);

  final String mensaje;
  final ApiErrorTipo tipo;

  @override
  String toString() => mensaje;
}
