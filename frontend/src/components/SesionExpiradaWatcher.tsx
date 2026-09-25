'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';
import { BACKEND_URL } from '@/lib/api';

/**
 * Detecta la expiración de sesión y devuelve al login.
 *
 * El token JWT dura 24 horas. Antes, cuando vencía, el backend respondía 403 a
 * todo y cada pantalla mostraba su propio "Error al obtener ..." con las tablas
 * vacías: el sistema parecía roto en lugar de pedir que se iniciara sesión de
 * nuevo. El middleware tampoco ayudaba, porque solo comprueba que la cookie
 * exista, no que el token siga siendo válido.
 *
 * Se resuelve interceptando fetch una sola vez en lugar de tocar las ~180
 * funciones de api.ts, para que ninguna quede sin cubrir por olvido.
 */
export default function SesionExpiradaWatcher() {
  const router = useRouter();

  useEffect(() => {
    const fetchOriginal = window.fetch;
    let redirigiendo = false;

    window.fetch = async (...args: Parameters<typeof fetch>) => {
      const respuesta = await fetchOriginal(...args);

      const url = typeof args[0] === 'string'
        ? args[0]
        : args[0] instanceof Request
          ? args[0].url
          : String(args[0]);

      const esLlamadaAlBackend = url.startsWith(BACKEND_URL);
      // Solo 401 (token inválido o vencido) es sesión expirada. Un 403 es un
      // usuario autenticado sin permiso para ESE endpoint puntual (por
      // ejemplo, un EMPLEADO pidiendo un dato solo-admin) y no debe cerrar
      // la sesión: cada pantalla se encarga de no mostrar lo que no le toca.
      const sesionRechazada = respuesta.status === 401;
      // El login rechazado no es una sesión vencida: lo maneja su propia pantalla.
      const esLogin = url.includes('/api/auth/login');

      if (esLlamadaAlBackend && sesionRechazada && !esLogin && !redirigiendo) {
        redirigiendo = true;
        Cookies.remove('token');
        Cookies.remove('user');
        Cookies.remove('role');
        // El motivo va en sessionStorage y no en la URL porque useAuth también
        // redirige al login al no encontrar la cookie, y pisaría el parámetro.
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
