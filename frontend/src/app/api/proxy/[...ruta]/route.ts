import { NextRequest, NextResponse } from 'next/server';
import { BACKEND_URL_INTERNAL } from '@/lib/backend-url';

async function manejarProxy(
  request: NextRequest,
  context: { params: Promise<{ ruta: string[] }> }
) {
  try {
    const { ruta } = await context.params;
    const rutaDestino = ruta.join('/');
    const searchParams = request.nextUrl.search;

    const token = request.cookies.get('token')?.value;
    const method = request.method;

    // Métodos mutantes requieren validación CSRF
    const esMutacion = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);
    if (esMutacion) {
      const csrfCookie = request.cookies.get('csrf-token')?.value;
      const csrfHeader = request.headers.get('x-csrf-token');

      // Si no coinciden o no existen, denegar por seguridad
      if (!csrfCookie || !csrfHeader || csrfCookie !== csrfHeader) {
        return NextResponse.json(
          { error: 'Token CSRF inválido o faltante' },
          { status: 403 }
        );
      }
    }

    const headersBackend: Record<string, string> = {};

    if (token) {
      headersBackend['Authorization'] = `Bearer ${token}`;
    }

    const contentType = request.headers.get('content-type');
    if (contentType) {
      headersBackend['Content-Type'] = contentType;
    }

    const urlBackend = `${BACKEND_URL_INTERNAL}/api/v1/${rutaDestino}${searchParams}`;

    let bodyData: BodyInit | undefined = undefined;
    if (['POST', 'PUT', 'PATCH'].includes(method)) {
      if (contentType?.includes('multipart/form-data')) {
        // En multipart pasar los bytes directos sin forzar content-type para preservar boundaries
        const arrayBuffer = await request.arrayBuffer();
        bodyData = arrayBuffer;
      } else {
        const text = await request.text();
        if (text) bodyData = text;
      }
    }

    const backendRes = await fetch(urlBackend, {
      method,
      headers: headersBackend,
      body: bodyData,
    });

    const respuestaHeaders = new Headers();
    const backendContentType = backendRes.headers.get('content-type');
    if (backendContentType) {
      respuestaHeaders.set('content-type', backendContentType);
    }

    const responseBlob = await backendRes.blob();
    const respuesta = new NextResponse(responseBlob, {
      status: backendRes.status,
      headers: respuestaHeaders,
    });

    // Si el backend devuelve 401 Unauthorized, limpiar la cookie de sesión
    if (backendRes.status === 401) {
      respuesta.cookies.delete('token');
      respuesta.cookies.delete('user_info');
      respuesta.cookies.delete('role');
    }

    return respuesta;
  } catch (error) {
    console.error('Error en BFF Proxy:', error);
    return NextResponse.json(
      { error: 'Error de comunicación con el backend' },
      { status: 502 }
    );
  }
}

export {
  manejarProxy as GET,
  manejarProxy as POST,
  manejarProxy as PUT,
  manejarProxy as PATCH,
  manejarProxy as DELETE,
};
