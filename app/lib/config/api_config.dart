/// URL base del backend. Nunca hardcodeada: se pasa al compilar/correr con
///   flutter run --dart-define-from-file=.env
/// (o --dart-define=API_URL=... a mano), leyendo app/.env, que se arma a
/// partir de app/.env.example.
///
/// Valor de emergencia: 10.0.2.2 es cómo el emulador de Android ve a
/// "localhost" de la computadora que lo corre. Solo se usa si no se pasó
/// nada al compilar.
class ApiConfig {
  ApiConfig._();

  static const String baseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  /// Donde se publica la última versión de la app (version.json): un bucket
  /// público de Supabase Storage. Ver scripts/publicar-apk.mjs.
  static const String actualizacionUrl = String.fromEnvironment(
    'UPDATE_URL',
    defaultValue: 'https://lcipybfksedtqrwgothl.supabase.co/storage/v1/object/public/app/version.json',
  );
}
