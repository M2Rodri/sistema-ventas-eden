import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const token = request.cookies.get('token')?.value;

  if (!token) {
    return NextResponse.json({ error: 'No autenticado' }, { status: 401 });
  }

  const userInfoCookie = request.cookies.get('user_info')?.value;
  if (userInfoCookie) {
    try {
      const user = JSON.parse(userInfoCookie);
      return NextResponse.json(user);
    } catch {
      // Ignorar fallo y extraer payload del JWT
    }
  }

  // Fallback: extraer usuario y rol desde payload base64 del JWT
  try {
    const partes = token.split('.');
    if (partes.length >= 2) {
      const payloadDecodificado = Buffer.from(partes[1], 'base64').toString('utf8');
      const payload = JSON.parse(payloadDecodificado);
      const role = (payload.role || '').replace('ROLE_', '').toUpperCase();
      return NextResponse.json({
        usuario: payload.sub,
        role,
      });
    }
  } catch {
    // Error al parsear token
  }

  return NextResponse.json({ error: 'Token inválido' }, { status: 401 });
}
