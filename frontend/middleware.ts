import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Páginas que quedaron fuera del alcance del proyecto. El código sigue en el repositorio,
 * pero nadie llega a ellas: la tienda pública (/tienda) manda al login y las del panel
 * (envíos, transportadoras, promociones y configuración) mandan al inicio del panel.
 */
const PAGINAS_FUERA_DE_ALCANCE = [
  '/dashboard/envios', // incluye /dashboard/envios/transportadoras
  '/dashboard/promociones',
  '/dashboard/configuracion',
];

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname;

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
      respuesta.cookies.delete('role');
      return respuesta;
    }

    const fueraDeAlcance = PAGINAS_FUERA_DE_ALCANCE.some(
      (ruta) => pathname === ruta || pathname.startsWith(ruta + '/'),
    );
    if (fueraDeAlcance) {
      return NextResponse.redirect(new URL('/dashboard', request.url));
    }

    return NextResponse.next();
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*', '/tienda/:path*', '/tienda'],
};
