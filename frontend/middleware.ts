import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname;

  // La tienda pública quedó fuera del alcance: sus direcciones mandan al login.
  if (pathname === '/tienda' || pathname.startsWith('/tienda/')) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  if (pathname.startsWith('/dashboard')) {
    const token = request.cookies.get('token')?.value;
    const role = request.cookies.get('role')?.value;

    if (!token) {
      return NextResponse.redirect(new URL('/login', request.url));
    }

    if (role !== 'ADMIN' && role !== 'EMPLEADO') {
      // Sin rol de panel no hay nada que ver aquí: se cierra la sesión y va al login.
      const respuesta = NextResponse.redirect(new URL('/login', request.url));
      respuesta.cookies.delete('token');
      respuesta.cookies.delete('user');
      respuesta.cookies.delete('user_info');
      respuesta.cookies.delete('role');
      respuesta.cookies.delete('csrf-token');
      return respuesta;
    }

    return NextResponse.next();
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*', '/tienda/:path*', '/tienda'],
};
