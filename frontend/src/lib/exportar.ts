/**
 * Exportación real a CSV, que Excel abre directamente.
 *
 * Antes los botones "Exportar a Excel" solo mostraban un alert diciendo
 * "Archivo generado exitosamente" sin generar ningún archivo. Acá se arma
 * el contenido de verdad y el navegador lo descarga.
 *
 * Se usa CSV y no .xlsx porque el formato de Excel es un ZIP con XML adentro
 * y necesitaría una librería externa; el CSV se abre igual con doble clic.
 */

/** Escapa un valor según RFC 4180: comillas dobles y separadores. */
function escaparCampo(valor: unknown): string {
  if (valor === null || valor === undefined) return '';
  const texto = String(valor);
  if (/[";\n\r]/.test(texto)) {
    return `"${texto.replace(/"/g, '""')}"`;
  }
  return texto;
}

export interface ColumnaExport<T> {
  encabezado: string;
  valor: (fila: T) => unknown;
}

/**
 * Descarga las filas como CSV.
 *
 * @param nombreArchivo nombre sin extensión
 * @param columnas encabezado + cómo sacar el dato de cada fila
 * @param filas los datos ya filtrados que se ven en pantalla
 * @returns cantidad de filas exportadas
 */
export function exportarCSV<T>(
  nombreArchivo: string,
  columnas: ColumnaExport<T>[],
  filas: T[]
): number {
  // Excel en español espera punto y coma como separador.
  const SEP = ';';

  const lineas = [
    columnas.map((c) => escaparCampo(c.encabezado)).join(SEP),
    ...filas.map((fila) =>
      columnas.map((c) => escaparCampo(c.valor(fila))).join(SEP)
    ),
  ];

  // El BOM hace que Excel reconozca UTF-8 y no rompa las tildes ni la ñ.
  const contenido = '\uFEFF' + lineas.join('\r\n');
  const blob = new Blob([contenido], { type: 'text/csv;charset=utf-8;' });

  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `${nombreArchivo}.csv`;
  document.body.appendChild(enlace);
  enlace.click();
  document.body.removeChild(enlace);
  URL.revokeObjectURL(url);

  return filas.length;
}

/** Fecha en formato AAAA-MM-DD para nombrar archivos. */
export function fechaArchivo(): string {
  return new Date().toISOString().split('T')[0];
}
