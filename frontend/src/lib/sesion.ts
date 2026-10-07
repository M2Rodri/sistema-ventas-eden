import Cookies from 'js-cookie';

/**
 * Sesión de la web: el token, los datos del usuario y su rol viajan en cookies.
 *
 * - Duran 12 horas, igual que el token del backend.
 * - SameSite=Strict: el navegador no las manda cuando la petición viene de otro sitio.
 * - Secure: solo viajan por HTTPS (los navegadores también las aceptan en localhost).
 *
 * No son HttpOnly porque las escribe el JavaScript de la página; por eso nada de esto se
 * guarda además en localStorage: sería un segundo lugar donde robar el token.
 */
const DOCE_HORAS_EN_DIAS = 0.5;

const OPCIONES = {
  expires: DOCE_HORAS_EN_DIAS,
  path: '/',
  sameSite: 'strict' as const,
  secure: true,
};

export function guardarSesion(token: string, usuario: unknown, rol: string): void {
  Cookies.set('token', token, OPCIONES);
  Cookies.set('user', JSON.stringify(usuario), OPCIONES);
  Cookies.set('role', rol, OPCIONES);
}

export function borrarSesion(): void {
  Cookies.remove('token', { path: '/' });
  Cookies.remove('user', { path: '/' });
  Cookies.remove('role', { path: '/' });
  // Versiones anteriores guardaban también aquí; se limpia lo que haya quedado.
  try {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  } catch {
    // sin acceso a localStorage: nada que limpiar
  }
}
