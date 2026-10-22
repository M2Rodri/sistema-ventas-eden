'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { cerrarSesion } from '@/lib/sesion';
import { INACTIVIDAD_MAXIMA_MS } from '@/lib/sesion-config';

const CLAVE_ULTIMA_ACTIVIDAD = 'ultimaActividad';
const EVENTOS = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'] as const;
const CADA_CUANTO_REVISAR_MS = 30_000;
const CADA_CUANTO_GUARDAR_MS = 5_000;

/**
 * Cierra la sesión cuando no se toca la pantalla durante INACTIVIDAD_MAXIMA_MS (30 minutos).
 *
 * Cubre el caso más realista: una computadora que alguien deja abierta. La última actividad
 * se guarda en localStorage para que valga entre pestañas: si se está usando otra pestaña, esta
 * no cierra la sesión. Si localStorage no está disponible, se usa una variable en memoria.
 */
export default function InactividadWatcher() {
  const router = useRouter();

  useEffect(() => {
    let enMemoria = Date.now();
    let ultimoGuardado = 0;
    let cerrando = false;

    const leer = (): number => {
      try {
        const guardada = Number(localStorage.getItem(CLAVE_ULTIMA_ACTIVIDAD));
        return guardada > 0 ? Math.max(guardada, enMemoria) : enMemoria;
      } catch {
        return enMemoria;
      }
    };

    const registrarActividad = () => {
      const ahora = Date.now();
      enMemoria = ahora;
      // No se escribe en cada movimiento del mouse: como mucho una vez cada pocos segundos.
      if (ahora - ultimoGuardado < CADA_CUANTO_GUARDAR_MS) return;
      ultimoGuardado = ahora;
      try {
        localStorage.setItem(CLAVE_ULTIMA_ACTIVIDAD, String(ahora));
      } catch {
        // sin localStorage: queda la variable en memoria
      }
    };

    const revisar = async () => {
      if (cerrando || Date.now() - leer() < INACTIVIDAD_MAXIMA_MS) return;
      cerrando = true;
      try {
        sessionStorage.setItem('motivoSalida', 'inactividad');
      } catch {
        // sin sessionStorage: igual se cierra la sesión, solo falta el aviso
      }
      await cerrarSesion();
      router.replace('/login');
    };

    // Al entrar cuenta como actividad.
    registrarActividad();

    EVENTOS.forEach((evento) => window.addEventListener(evento, registrarActividad, { passive: true }));
    const intervalo = setInterval(() => { void revisar(); }, CADA_CUANTO_REVISAR_MS);
    // Una computadora que estuvo suspendida no dispara el intervalo a tiempo: se revisa al volver.
    const alVolver = () => { if (document.visibilityState === 'visible') void revisar(); };
    document.addEventListener('visibilitychange', alVolver);

    return () => {
      EVENTOS.forEach((evento) => window.removeEventListener(evento, registrarActividad));
      clearInterval(intervalo);
      document.removeEventListener('visibilitychange', alVolver);
    };
  }, [router]);

  return null;
}
