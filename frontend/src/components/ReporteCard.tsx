'use client';

import { ConfiguracionReporte } from '@/types/reporte';
import { ChevronRight } from 'lucide-react';

interface ReporteCardProps {
  config: ConfiguracionReporte;
  IconComponent: React.ComponentType<{ size?: number; className?: string }>;
  onGenerar: () => void;
}

export default function ReporteCard({ config, IconComponent, onGenerar }: ReporteCardProps) {
  return (
    // Dos capas a propósito: la de afuera lleva la sombra y el levantado del
    // hover, sin overflow-hidden (si lo tuviera, se recortaría su propia
    // sombra contra su propio borde y no se vería nada). La de adentro lleva
    // el overflow-hidden, para que la barra verde y la marca de agua queden
    // recortadas contra las esquinas redondeadas.
    <div
      className="reporte-card group relative h-full rounded-xl cursor-pointer"
      onClick={onGenerar}
    >
      <div className="relative overflow-hidden h-full flex flex-col rounded-xl bg-[#F7FCFB] px-6 py-5">
        {/* Barra superior */}
        <div className="absolute top-0 left-0 right-0 h-[5px] rounded-t-xl bg-[#0D8C80]" />

        {/* Marca de agua: el ícono de la tarjeta, chico y tenue, en la franja vacía sobre el botón */}
        <IconComponent
          size={48}
          className="absolute right-[6px] bottom-[88px] opacity-[0.16] text-[#0D8C80] pointer-events-none"
        />

        <div className="relative z-10 flex flex-col flex-1">
          {/* Título: alto fijo de dos líneas, para que lo de abajo arranque
              siempre a la misma altura, tenga el título una línea o dos. */}
          <h3 className="text-lg font-bold mb-2 text-[#0B6B62] h-14 line-clamp-2">
            {config.titulo}
          </h3>

          {/* Descripción */}
          <p className="text-sm mb-4 line-clamp-2 text-[#5B6B72]">
            {config.descripcion}
          </p>

          {/* Categorías */}
          <div className="flex flex-wrap gap-2 mb-4">
            {config.categorias.map((cat, index) => (
              <span key={index} className="text-xs px-2 py-1 rounded-full bg-[#E4F2EF] border border-[#CFE6E2] text-[#0B6B62]">
                {cat}
              </span>
            ))}
          </div>

          {/* Características */}
          <div className="flex items-center gap-3 mb-5 text-xs text-[#5B6B72]">
            {config.requiereFechas && (
              <span className="flex items-center gap-1">
                📅 Rango de fechas
              </span>
            )}
            {config.requiereLimite && (
              <span className="flex items-center gap-1">
                🔢 Límite configurable
              </span>
            )}
          </div>

          {/* Botón */}
          <button className="mt-auto w-full flex items-center justify-center gap-2 bg-[#0D8C80] group-hover:bg-[#0B6B62] text-white py-2.5 rounded-[10px] font-medium transition-all duration-300">
            Generar Reporte
            <ChevronRight size={18} className="group-hover:translate-x-1 transition-transform duration-300" />
          </button>
        </div>
      </div>
    </div>
  );
}