import { NextRequest, NextResponse } from 'next/server';
import { BACKEND_URL_INTERNAL } from '@/lib/backend-url';
import { aplicarCookiesDeSesion } from '@/lib/cookies-sesion';
import { DURACION_SESION_RESPALDO_SEGUNDOS, UMBRAL_RENOVACION_SEGUNDOS } from '@/lib/sesion-config';
import { segundosRestantes } from '@/lib/token-servidor';

// Renovaciones que ya están en marcha, por token. Al abrir una pantalla salen varias
// peticiones juntas: con esto solo una de ellas pide la renovación y las demás esperan esa misma.
const renovacionesEnCurso = new Map<string, Promise<Record<string, any> | null>>();

function pedirRenovacion(token: string): Promise<Record<string, any> | null> {
  const enCurso = renovacionesEnCurso.get(token);
  if (enCurso) return enCurso;

  const promesa = (async () => {
    try {
      const res = await fetch(`${BACKEND_URL_INTERNAL}/api/v1/auth/renovar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        signal: AbortSignal.timeout(5000),
      });
      return res.ok ? await res.json() : null;
    } catch {
      return null; // la renovación es una comodidad: si falla, la sesión sigue hasta que venza sola
    }
  })();

  renovacionesEnCurso.set(token, promesa);
  setTimeout(() => renovacionesEnCurso.delete(token), 10_000);
  return promesa;
}

/** Si al token le queda poco, pide uno nuevo al backend y deja las cookies al día en la respuesta. */
async function renovarSesionSiHaceFalta(
  request: NextRequest,
  respuesta: NextResponse,
  token: string
): Promise<void> {
  const restantes = segundosRestantes(token);
  if (restantes === null || restantes > UMBRAL_RENOVACION_SEGUNDOS) return;

  const nueva = await pedirRenovacion(token);
  if (!nueva?.token) return;

  const { token: nuevoToken, role, ...restoUsuario } = nueva;
  const rolLimpio = String(role || '').replace('ROLE_', '').toUpperCase();
  const maxAge = segundosRestantes(nuevoToken) ?? DURACION_SESION_RESPALDO_SEGUNDOS;
  const csrfToken = request.cookies.get('csrf-token')?.value ?? crypto.randomUUID();

  aplicarCookiesDeSesion(respuesta, nuevoToken, { ...restoUsuario, role: rolLimpio }, maxAge, csrfToken);
}

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

    // Un 204 no admite cuerpo: armarlo con uno haría fallar la respuesta.
    const responseBlob = [204, 205, 304].includes(backendRes.status) ? null : await backendRes.blob();
    const respuesta = new NextResponse(responseBlob, {
      status: backendRes.status,
      headers: respuestaHeaders,
    });

    // Si el backend devuelve 401 Unauthorized, limpiar la cookie de sesión
    if (backendRes.status === 401) {
      respuesta.cookies.delete('token');
      respuesta.cookies.delete('user_info');
      respuesta.cookies.delete('role');
    } else if (token) {
      // Mientras se usa el sistema, la sesión se alarga sola (con el tope que fija el backend).
      await renovarSesionSiHaceFalta(request, respuesta, token);
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
