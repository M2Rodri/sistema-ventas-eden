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
    <div className="group bg-white border-2 border-gray-200 rounded-xl p-6 hover:border-primary-500 hover:shadow-xl transition-all duration-300 cursor-pointer"
         onClick={onGenerar}>
      {/* Header con Ícono */}
      <div className={`w-14 h-14 rounded-xl bg-gradient-to-br ${config.color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300`}>
        <IconComponent size={28} className="text-white" />
      </div>

      {/* Título */}
      <h3 className="text-lg font-bold text-gray-900 mb-2 group-hover:text-primary-600 transition-colors">
        {config.titulo}
      </h3>

      {/* Descripción */}
      <p className="text-sm text-gray-600 mb-4 line-clamp-2">
        {config.descripcion}
      </p>

      {/* Categorías */}
      <div className="flex flex-wrap gap-2 mb-4">
        {config.categorias.map((cat, index) => (
          <span key={index} className="text-xs px-2 py-1 bg-gray-100 text-gray-700 rounded-full">
            {cat}
          </span>
        ))}
      </div>

      {/* Características */}
      <div className="flex items-center gap-3 mb-4 text-xs text-gray-500">
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
      <button className="w-full flex items-center justify-center gap-2 bg-gray-100 group-hover:bg-primary-600 text-gray-700 group-hover:text-white py-2.5 rounded-lg font-medium transition-all duration-300">
        Generar Reporte
        <ChevronRight size={18} className="group-hover:translate-x-1 transition-transform duration-300" />
      </button>
    </div>
  );
}