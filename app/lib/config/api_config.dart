/// URL base del backend.
class ApiConfig {
  ApiConfig._();

  static const String baseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  /// Donde se publica la última versión de la app (version.json): un bucket público de Supabase Storage.
  static const String actualizacionUrl = String.fromEnvironment(
    'UPDATE_URL',
    defaultValue:
        'https://lcipybfksedtqrwgothl.supabase.co/storage/v1/object/public/app/version.json',
  );
}
