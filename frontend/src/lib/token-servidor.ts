/** Lectura del vencimiento de un JWT, solo para uso en el servidor (rutas /api). */
export function segundosRestantes(token: string): number | null {
  try {
    const partes = token.split('.');
    if (partes.length !== 3) return null;
    const payload = JSON.parse(Buffer.from(partes[1], 'base64url').toString('utf8'));
    if (typeof payload.exp !== 'number') return null;
    return Math.floor(payload.exp - Date.now() / 1000);
  } catch {
    return null;
  }
}
