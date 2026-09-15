/**
 * Carga tolerante a fallos para las pantallas que piden varios endpoints a la vez.
 *
 * El problema que resuelve: estas pantallas usaban Promise.all, que rechaza
 * apenas falla UNA de las llamadas. Si se caía un endpoint secundario (las
 * estadísticas, las alertas, el listado de categorías), la pantalla entera
 * quedaba en blanco aunque el dato principal se hubiera traído bien.
 *
 * Con Promise.allSettled se muestra todo lo que sí llegó y se avisa por
 * separado qué no se pudo traer.
 */

/** Junta los resultados de un Promise.allSettled y anota cuáles fallaron. */
export function crearRecolector() {
  const fallos: string[] = [];

  /**
   * Devuelve el valor si la promesa se resolvió; si falló, anota el nombre
   * legible de la sección y devuelve el valor por defecto.
   *
   * @param resultado lo que devolvió Promise.allSettled para esa posición
   * @param nombre cómo llamar a esa sección en el aviso al usuario
   * @param porDefecto qué usar cuando la llamada falló (normalmente [] o 0)
   */
  function tomar<T>(
    resultado: PromiseSettledResult<T>,
    nombre: string,
    porDefecto: T
  ): T {
    if (resultado.status === 'fulfilled') {
      return resultado.value;
    }
    fallos.push(nombre);
    // Queda en consola para poder diagnosticar; al usuario se le muestra el aviso.
    console.error(`No se pudo cargar ${nombre}:`, resultado.reason);
    return porDefecto;
  }

  return { tomar, fallos };
}
