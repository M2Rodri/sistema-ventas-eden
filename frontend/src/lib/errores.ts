/**
 * Traduce un error de una llamada a la API a un mensaje que el usuario
 * pueda entender.
 *
 * Cuando el backend no responde (apagado, sin red, CORS), fetch() rechaza
 * con un TypeError genérico del navegador ("Failed to fetch", "NetworkError
 * when attempting to fetch resource", "Load failed", según el navegador).
 * Mostrar ese texto tal cual no le dice nada al usuario, así que se
 * reemplaza por un mensaje de conexión.
 *
 * Cuando el backend sí respondió pero con un error (404, 400, 500...), las
 * funciones de src/lib/api.ts ya lanzan un Error con el mensaje propio del
 * servidor, y ese se muestra sin modificar.
 */
export function mensajeError(
  error: unknown,
  fallback = 'Ocurrió un error inesperado.'
): string {
  if (error instanceof TypeError) {
    return 'No se pudo conectar con el servidor. Verifique tu conexión e intentá de nuevo.';
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}
