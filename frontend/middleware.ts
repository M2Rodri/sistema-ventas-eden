import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname;

  if (pathname.startsWith('/dashboard')) {
    const token = request.cookies.get('token')?.value;
    const role = request.cookies.get('role')?.value;

    if (!token) {
      return NextResponse.redirect(new URL('/login', request.url));
    }

    if (role !== 'ADMIN' && role !== 'EMPLEADO') {
      return NextResponse.redirect(new URL('/tienda', request.url));
    }

    return NextResponse.next();
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*'],
};