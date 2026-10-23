/** Sesión de la web. */

export interface DatosUsuario {
  id: number;
  nombre: string;
  apellido: string;
  usuario: string;
  role: string;
}

/** Inicia sesión via el BFF. Devuelve los datos del usuario (sin token). */
export async function iniciarSesion(usuario: string, password: string): Promise<DatosUsuario> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usuario, password }),
  });

  if (!res.ok) {
    const errorBody = await res.json().catch(() => null);
    const mensaje = errorBody?.error || errorBody?.mensaje || 'Error al iniciar sesión';
    throw new Error(mensaje);
  }

  return res.json();
}

/** Cierra la sesión via el BFF. */
export async function cerrarSesion(): Promise<void> {
  try {
    await fetch('/api/auth/logout', { method: 'POST' });
  } catch {
    // Si falla la red, proseguir con limpieza local
  }
  borrarSesionLocal();
}

/** Lee los datos del usuario de la cookie `user_info` (no HttpOnly). */
export function obtenerUsuarioDeCookie(): DatosUsuario | null {
  if (typeof document === 'undefined') return null;
  const cookie = document.cookie
    .split('; ')
    .find((row) => row.startsWith('user_info='));
  if (!cookie) return null;
  try {
    return JSON.parse(decodeURIComponent(cookie.split('=').slice(1).join('=')));
  } catch {
    return null;
  }
}

/**
 * Borrar sesión de forma local en el navegador.
 * Limpia las cookies accesibles por JavaScript y restos antiguos de localStorage.
 */
export function borrarSesionLocal(): void {
  if (typeof document !== 'undefined') {
    document.cookie = 'user_info=; path=/; max-age=0';
    document.cookie = 'role=; path=/; max-age=0';
    document.cookie = 'csrf-token=; path=/; max-age=0';
  }
  try {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  } catch {
    // sin acceso a localStorage: nada que limpiar
  }
}

// Alias de compatibilidad — los imports existentes que usen borrarSesion siguen funcionando.
export const borrarSesion = borrarSesionLocal;
