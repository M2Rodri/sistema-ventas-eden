/**
 * Tiempos de la sesión de la web. Se importan desde el servidor (rutas /api) y desde el navegador.
 *
 * La vida real de cada token la fija el backend (`jwt.expiration`, 4 horas); la web la lee del propio
 * token. Acá quedan el valor de respaldo (por si no se pudiera leer) y los umbrales propios de la web.
 */

/** Respaldo: igual a `jwt.expiration` del backend (4 horas). */
export const DURACION_SESION_RESPALDO_SEGUNDOS = 4 * 60 * 60;

/** Cuando al token le queda esta cantidad de segundos o menos, la próxima petición lo renueva. */
export const UMBRAL_RENOVACION_SEGUNDOS = 60 * 60;

/** Sin tocar la pantalla durante este tiempo, se cierra la sesión. */
export const INACTIVIDAD_MAXIMA_MS = 30 * 60 * 1000;
