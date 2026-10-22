import type { NextResponse } from 'next/server';

/**
 * Cookies de la sesión web, solo para uso en el servidor (rutas /api).
 * Las usan el inicio de sesión y la renovación del token, para que ambas queden idénticas.
 *
 *  - token:      el JWT; HttpOnly, el código del navegador nunca lo ve.
 *  - user_info:  datos del usuario para mostrar en pantalla (sin secretos).
 *  - role:       el rol, para el middleware de Next.js.
 *  - csrf-token: valor que la web reenvía en la cabecera X-CSRF-Token en las peticiones que modifican.
 */
export function aplicarCookiesDeSesion(
  response: NextResponse,
  token: string,
  usuario: { role: string } & Record<string, unknown>,
  maxAge: number,
  csrfToken: string
): void {
  const isProd = process.env.NODE_ENV === 'production';
  const base = { secure: isProd, sameSite: 'lax' as const, path: '/', maxAge };

  response.cookies.set('token', token, { ...base, httpOnly: true });
  response.cookies.set('user_info', JSON.stringify(usuario), { ...base, httpOnly: false });
  response.cookies.set('role', usuario.role, { ...base, httpOnly: false });
  response.cookies.set('csrf-token', csrfToken, { ...base, httpOnly: false });
}
