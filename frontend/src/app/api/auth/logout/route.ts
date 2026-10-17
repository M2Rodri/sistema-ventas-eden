import { NextResponse } from 'next/server';

export async function POST() {
  const response = NextResponse.json({ ok: true, mensaje: 'Sesión cerrada exitosamente' });

  // Limpiar cookies
  response.cookies.delete('token');
  response.cookies.delete('user_info');
  response.cookies.delete('role');
  response.cookies.delete('csrf-token');

  return response;
}
