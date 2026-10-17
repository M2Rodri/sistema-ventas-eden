import { NextRequest, NextResponse } from 'next/server';
import { BACKEND_URL_INTERNAL } from '@/lib/backend-url';

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

    const isProd = process.env.NODE_ENV === 'production';
    const maxAge = 43200; // 12 horas en segundos

    // Cookie segura con el token JWT (HttpOnly)
    response.cookies.set('token', token, {
      httpOnly: true,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge,
    });

    // Cookie con datos legibles por el cliente (sin secretos ni token)
    response.cookies.set(
      'user_info',
      JSON.stringify({
        ...restoUsuario,
        role: rolLimpio,
      }),
      {
        httpOnly: false,
        secure: isProd,
        sameSite: 'lax',
        path: '/',
        maxAge,
      }
    );

    // Cookie con el rol para el middleware de Next.js
    response.cookies.set('role', rolLimpio, {
      httpOnly: false,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge,
    });

    // Token CSRF generado para mitigar ataques CSRF en peticiones mutantes
    const csrfToken = crypto.randomUUID();
    response.cookies.set('csrf-token', csrfToken, {
      httpOnly: false,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge,
    });

    return response;
  } catch (error) {
    console.error('Error en BFF /api/auth/login:', error);
    return NextResponse.json(
      { error: 'Error interno en el servidor de autenticación' },
      { status: 500 }
    );
  }
}
