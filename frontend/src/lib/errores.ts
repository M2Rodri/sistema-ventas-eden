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
/**
 * Error que devuelve la API. Formato único de la API:
 *   { "error": { "codigo": "VENTA_NO_ENCONTRADA", "mensaje": "...", "campos": { "campo": "detalle" } } }
 * "campos" solo viene en errores de validación. Mientras el backend publicado
 * todavía use el formato anterior ({ "error": "texto" }), también se lee ese.
 */
export class ErrorApi extends Error {
  constructor(
    mensaje: string,
    public readonly estado?: number,
    public readonly codigo?: string,
    public readonly campos?: Record<string, string>
  ) {
    super(mensaje);
    this.name = 'ErrorApi';
  }
}

/** Extrae el mensaje de un cuerpo de error, en el formato nuevo o en el anterior. */
export function mensajeDeCuerpo(cuerpo: unknown, fallback: string): string {
  const error = (cuerpo as { error?: unknown } | null | undefined)?.error;
  if (typeof error === 'string' && error) return error;
  const mensaje = (error as { mensaje?: unknown } | undefined)?.mensaje;
  if (typeof mensaje === 'string' && mensaje) return mensaje;
  return fallback;
}

/** Arma el ErrorApi de una respuesta con error. Si el cuerpo no es JSON, usa el texto de respaldo. */
export async function errorDeRespuesta(response: Response, fallback: string): Promise<ErrorApi> {
  let cuerpo: unknown = null;
  try {
    cuerpo = JSON.parse(await response.text());
  } catch {
    // cuerpo vacío o que no es JSON
  }
  const error = (cuerpo as { error?: { codigo?: string; campos?: Record<string, string> } } | null)?.error;
  const detalle = typeof error === 'object' && error ? error : undefined;
  return new ErrorApi(mensajeDeCuerpo(cuerpo, fallback), response.status, detalle?.codigo, detalle?.campos);
}

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
