'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { borrarSesionLocal } from '@/lib/sesion';

/**
 * Detecta la expiración de sesión y devuelve al login.
 *
 * El token JWT dura 4 horas y se renueva solo mientras se usa el sistema (ver el proxy en
 * /api/proxy), con un tope de 8 horas por sesión. Cuando vence, el backend responde 401.
 * Con el BFF proxy, las llamadas al backend pasan por /api/proxy/...
 */
export default function SesionExpiradaWatcher() {
  const router = useRouter();

  useEffect(() => {
    const fetchOriginal = window.fetch;
    let redirigiendo = false;

    window.fetch = async (...args: Parameters<typeof fetch>) => {
      const respuesta = await fetchOriginal(...args);

      const url =
        typeof args[0] === 'string'
          ? args[0]
          : args[0] instanceof Request
          ? args[0].url
          : String(args[0]);

      const esLlamadaAlProxy = url.startsWith('/api/proxy') || url.includes('/api/proxy');
      const sesionRechazada = respuesta.status === 401;
      const esLogin = url.includes('/api/auth/login');

      if (esLlamadaAlProxy && sesionRechazada && !esLogin && !redirigiendo) {
        redirigiendo = true;
        borrarSesionLocal();
        sessionStorage.setItem('motivoSalida', 'sesion-expirada');
        router.replace('/login');
      }

      return respuesta;
    };

    return () => {
      window.fetch = fetchOriginal;
    };
  }, [router]);

  return null;
}
