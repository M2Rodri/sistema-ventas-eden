import { NextRequest, NextResponse } from 'next/server';
import { BACKEND_URL_INTERNAL } from '@/lib/backend-url';
import { aplicarCookiesDeSesion } from '@/lib/cookies-sesion';
import { DURACION_SESION_RESPALDO_SEGUNDOS } from '@/lib/sesion-config';
import { segundosRestantes } from '@/lib/token-servidor';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { usuario, password } = body;

    if (!usuario || !password) {
      return NextResponse.json(
        { error: 'Usuario y contraseña son requeridos' },
        { status: 400 }
      );
    }

    const backendRes = await fetch(`${BACKEND_URL_INTERNAL}/api/v1/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ usuario, password }),
    });

    const data = await backendRes.json().catch(() => null);

    if (!backendRes.ok) {
      return NextResponse.json(
        data || { error: 'Error al iniciar sesión' },
        { status: backendRes.status }
      );
    }

    const { token, role, ...restoUsuario } = data;
    const rolLimpio = (role || '').replace('ROLE_', '').toUpperCase();

    const response = NextResponse.json({
      role: rolLimpio,
      ...restoUsuario,
    });

    // Las cookies duran lo mismo que el token: se lee del propio token (lo fija el backend).
    const maxAge = segundosRestantes(token) ?? DURACION_SESION_RESPALDO_SEGUNDOS;

    // Token CSRF generado para mitigar ataques CSRF en peticiones mutantes
    const csrfToken = crypto.randomUUID();

    aplicarCookiesDeSesion(response, token, { ...restoUsuario, role: rolLimpio }, maxAge, csrfToken);

    return response;
  } catch (error) {
    console.error('Error en BFF /api/auth/login:', error);
    return NextResponse.json(
      { error: 'Error interno en el servidor de autenticación' },
      { status: 500 }
    );
  }
}
