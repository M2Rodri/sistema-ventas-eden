'use client';

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { Bell, AlertTriangle, Mail, RefreshCw } from 'lucide-react';
import { getAlertasPendientes, getMensajesContacto } from '@/lib/api';
import { AlertaInventario } from '@/types/inventario';
import { MensajeContacto } from '@/lib/api';

/**
 * Campana de notificaciones.
 *
 * Antes era un botón sin comportamiento con un punto rojo escrito en el HTML:
 * siempre parecía haber avisos sin leer, y al hacer clic no pasaba nada.
 *
 * Ahora muestra las dos cosas del sistema que realmente reclaman atención de
 * quien administra, y que hasta hoy solo se veían entrando a la pantalla
 * correspondiente:
 *
 *   - Productos por debajo del stock mínimo (alertas_inventario).
 *   - Consultas de la tienda que nadie atendió todavía (mensajes_contacto).
 *
 * Si no hay ninguna, no se pinta el punto rojo. Un indicador que está siempre
 * encendido deja de significar algo.
 */

/** Cada cuánto se vuelven a pedir los avisos, en milisegundos. */
const INTERVALO_REFRESCO = 60_000;

export default function NotificacionesMenu() {
  const [alertas, setAlertas] = useState<AlertaInventario[]>([]);
  const [mensajes, setMensajes] = useState<MensajeContacto[]>([]);
  const [abierto, setAbierto] = useState(false);
  const [cargando, setCargando] = useState(true);
  const contenedor = useRef<HTMLDivElement>(null);

  const cargar = async () => {
    setCargando(true);
    // allSettled: si se cae uno de los dos endpoints, el otro tipo de aviso
    // igual se muestra. Los errores no se le cantan al usuario porque esto es
    // un accesorio de la cabecera, no la pantalla que vino a usar.
    const [rAlertas, rMensajes] = await Promise.allSettled([
      getAlertasPendientes(),
      getMensajesContacto(),
    ]);

    if (rAlertas.status === 'fulfilled') {
      setAlertas(rAlertas.value);
    } else {
      console.error('No se pudieron cargar las alertas de stock:', rAlertas.reason);
    }

    if (rMensajes.status === 'fulfilled') {
      setMensajes(rMensajes.value.filter((m) => !m.atendido));
    } else {
      console.error('No se pudieron cargar los mensajes de contacto:', rMensajes.reason);
    }

    setCargando(false);
  };

  useEffect(() => {
    cargar();
    // Se relee cada tanto para que una consulta que entra por la tienda aparezca
    // sin obligar a recargar la página.
    const temporizador = setInterval(cargar, INTERVALO_REFRESCO);
    return () => clearInterval(temporizador);
  }, []);

  // Cerrar al hacer clic fuera del menú.
  useEffect(() => {
    if (!abierto) return;
    const alClicar = (e: MouseEvent) => {
      if (contenedor.current && !contenedor.current.contains(e.target as Node)) {
        setAbierto(false);
      }
    };
    document.addEventListener('mousedown', alClicar);
    return () => document.removeEventListener('mousedown', alClicar);
  }, [abierto]);

  const total = alertas.length + mensajes.length;

  const formatearFecha = (fecha: string) =>
    new Date(fecha).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });

  return (
    <div className="relative" ref={contenedor}>
      <button
        onClick={() => setAbierto(!abierto)}
        className="relative rounded-lg p-2 transition-colors hover:bg-gray-100"
        title={total > 0 ? `${total} aviso${total === 1 ? '' : 's'} sin atender` : 'Sin avisos pendientes'}
        aria-label="Notificaciones"
      >
        <Bell size={20} className="text-gray-600" />
        {/* El indicador aparece solo si de verdad hay algo pendiente. */}
        {total > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-red-500 px-1 text-[11px] font-bold text-white">
            {total > 9 ? '9+' : total}
          </span>
        )}
      </button>

      {abierto && (
        <div className="absolute right-0 z-50 mt-2 w-96 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3">
            <p className="font-semibold text-gray-900">Avisos pendientes</p>
            <button
              onClick={cargar}
              className="rounded p-1 text-gray-500 transition-colors hover:bg-gray-200 hover:text-gray-700"
              title="Actualizar"
            >
              <RefreshCw size={16} className={cargando ? 'animate-spin' : ''} />
            </button>
          </div>

          <div className="max-h-96 overflow-y-auto">
            {cargando && total === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-gray-500">Cargando…</p>
            ) : total === 0 ? (
              <div className="px-4 py-8 text-center">
                <Bell size={32} className="mx-auto mb-2 text-gray-300" />
                <p className="text-sm text-gray-500">No hay nada pendiente</p>
              </div>
            ) : (
              <>
                {alertas.length > 0 && (
                  <div>
                    <p className="bg-yellow-50 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-yellow-800">
                      Stock bajo ({alertas.length})
                    </p>
                    {alertas.map((a) => (
                      <Link
                        key={`alerta-${a.id}`}
                        href={`/dashboard/inventario?producto=${a.idProducto}`}
                        onClick={() => setAbierto(false)}
                        className="flex gap-3 border-b border-gray-100 px-4 py-3 transition-colors hover:bg-gray-50"
                      >
                        <AlertTriangle size={18} className="mt-0.5 flex-shrink-0 text-yellow-600" />
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-gray-900">
                            {a.nombreProducto}
                          </p>
                          <p className="text-xs text-gray-600">
                            Quedan {a.cantidadActual} y el mínimo es {a.cantidadMinima}
                          </p>
                          <p className="mt-0.5 text-xs text-gray-400">
                            {formatearFecha(a.fechaAlerta)}
                          </p>
                        </div>
                      </Link>
                    ))}
                  </div>
                )}

                {mensajes.length > 0 && (
                  <div>
                    <p className="bg-blue-50 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-blue-800">
                      Consultas sin responder ({mensajes.length})
                    </p>
                    {mensajes.map((m) => (
                      <Link
                        key={`mensaje-${m.id}`}
                        href="/dashboard/configuracion?tab=mensajes"
                        onClick={() => setAbierto(false)}
                        className="flex gap-3 border-b border-gray-100 px-4 py-3 transition-colors hover:bg-gray-50"
                      >
                        <Mail size={18} className="mt-0.5 flex-shrink-0 text-blue-600" />
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-gray-900">{m.asunto}</p>
                          <p className="truncate text-xs text-gray-600">De {m.nombre}</p>
                          <p className="mt-0.5 text-xs text-gray-400">
                            {formatearFecha(m.fechaEnvio)}
                          </p>
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
