// Ayudas para adjuntar el comprobante de pago: validar el archivo antes de
// subirlo, tomar una imagen pegada desde el portapapeles (captura de pantalla,
// "Copiar imagen" del navegador o de WhatsApp Web) y recibir un archivo
// arrastrado.
//
// Son los mismos límites que revisa el servidor (ValidadorImagen): JPG, JPEG,
// JFIF, PNG, WebP o PDF, de hasta 10 MB.

export const EXTENSIONES_COMPROBANTE = ['.jpg', '.jpeg', '.jfif', '.png', '.webp', '.pdf'];
export const ACCEPT_COMPROBANTE = EXTENSIONES_COMPROBANTE.join(',');
export const MAXIMO_COMPROBANTE_MB = 10;
export const TEXTO_FORMATOS_COMPROBANTE = `JPG, JPEG, JFIF, PNG, WebP o PDF, de hasta ${MAXIMO_COMPROBANTE_MB} MB`;

const TIPOS_COMPROBANTE = ['image/jpeg', 'image/jpg', 'image/pjpeg', 'image/png', 'image/webp', 'application/pdf'];

const EXTENSION_POR_TIPO: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

function extensionDe(nombre: string): string {
  const punto = nombre.lastIndexOf('.');
  return punto < 0 ? '' : nombre.slice(punto).toLowerCase();
}

/** Devuelve el motivo si el archivo no sirve como comprobante, o null si está bien. */
export function errorDeComprobante(archivo: File): string | null {
  // El navegador no siempre informa el tipo de un .jfif: en ese caso se mira la extensión.
  const tipoValido = archivo.type ? TIPOS_COMPROBANTE.includes(archivo.type) : true;
  if (!tipoValido || !EXTENSIONES_COMPROBANTE.includes(extensionDe(archivo.name))) {
    return `Ese archivo no se puede usar como comprobante. Solo se aceptan archivos ${TEXTO_FORMATOS_COMPROBANTE}.`;
  }
  if (archivo.size > MAXIMO_COMPROBANTE_MB * 1024 * 1024) {
    return `El archivo pesa más de ${MAXIMO_COMPROBANTE_MB} MB. Elegí uno más liviano.`;
  }
  return null;
}

export function esPdf(archivo: File): boolean {
  return archivo.type === 'application/pdf' || extensionDe(archivo.name) === '.pdf';
}

/** Para un enlace de comprobante ya guardado (puede traer parámetros de firma al final). */
export function urlEsPdf(url: string): boolean {
  return /\.pdf($|\?)/i.test(url);
}

/** Las imágenes pegadas llegan con nombre genérico ("image.png"): se les pone uno claro. */
function conNombreDePegado(archivo: File): File {
  const extension = EXTENSION_POR_TIPO[archivo.type] ?? 'png';
  return new File([archivo], `comprobante-pegado.${extension}`, { type: archivo.type });
}

/** Imagen que viene en un evento de pegar (Ctrl+V), o null si lo pegado no es una imagen. */
export function imagenDeEventoPegar(evento: ClipboardEvent): File | null {
  const items = evento.clipboardData?.items;
  if (!items) return null;
  for (const item of Array.from(items)) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      const archivo = item.getAsFile();
      if (archivo) return conNombreDePegado(archivo);
    }
  }
  return null;
}

/**
 * Imagen que hay en el portapapeles ahora mismo (para un botón "Pegar imagen").
 * El navegador pide permiso la primera vez. Devuelve null si no hay imagen.
 */
export async function imagenDelPortapapeles(): Promise<File | null> {
  if (!navigator.clipboard?.read) {
    throw new Error('Este navegador no permite pegar con el botón. Probá con Ctrl+V.');
  }
  const elementos = await navigator.clipboard.read();
  for (const elemento of elementos) {
    const tipo = elemento.types.find((t) => t.startsWith('image/'));
    if (tipo) {
      const blob = await elemento.getType(tipo);
      return conNombreDePegado(new File([blob], 'pegado', { type: tipo }));
    }
  }
  return null;
}

/** Primer archivo que llega en un evento de arrastrar y soltar, o null. */
export function archivoDeEventoSoltar(evento: { dataTransfer: DataTransfer | null }): File | null {
  return evento.dataTransfer?.files?.[0] ?? null;
}
