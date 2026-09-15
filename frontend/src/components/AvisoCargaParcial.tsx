'use client';

import { AlertTriangle, RefreshCw } from 'lucide-react';

interface AvisoCargaParcialProps {
  /** Nombres legibles de las secciones que no se pudieron cargar. */
  fallos: string[];
  /** Vuelve a intentar la carga completa. */
  onReintentar: () => void;
}

/**
 * Aviso fijo para cuando una pantalla cargó a medias.
 *
 * Se muestra arriba de todo y no desaparece solo, a diferencia de los mensajes
 * de éxito que se van a los pocos segundos: si falta un dato, el usuario tiene
 * que poder verlo mientras mira la pantalla, no durante cuatro segundos.
 *
 * Importa sobre todo para no confundir "no hay registros" con "no pude traer
 * los registros", que en pantalla se ven igual.
 */
export default function AvisoCargaParcial({ fallos, onReintentar }: AvisoCargaParcialProps) {
  if (fallos.length === 0) return null;

  const lista =
    fallos.length === 1
      ? fallos[0]
      : `${fallos.slice(0, -1).join(', ')} y ${fallos[fallos.length - 1]}`;

  return (
    <div className="mb-4 flex items-start gap-3 rounded-lg border border-yellow-300 bg-yellow-50 p-4">
      <AlertTriangle className="mt-0.5 flex-shrink-0 text-yellow-600" size={20} />
      <div className="flex-1">
        <p className="text-sm font-semibold text-yellow-900">
          La pantalla se cargó de forma incompleta
        </p>
        <p className="mt-1 text-sm text-yellow-800">
          No se pudo cargar {lista}. Lo demás que ves acá sí está actualizado.
        </p>
      </div>
      <button
        onClick={onReintentar}
        className="flex flex-shrink-0 items-center gap-2 rounded-lg bg-yellow-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-yellow-700"
      >
        <RefreshCw size={16} />
        Reintentar
      </button>
    </div>
  );
}
