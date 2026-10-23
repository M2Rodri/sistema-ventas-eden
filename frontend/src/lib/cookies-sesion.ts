import type { NextResponse } from 'next/server';

/** Cookies de la sesión web, solo para uso en el servidor (rutas /api). */
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
