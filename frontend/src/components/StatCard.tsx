import React from 'react';

interface StatCardProps {
  titulo: string;
  valor: React.ReactNode;
  subtitulo?: React.ReactNode;
  icon: React.ReactNode;
  onClick?: () => void;
  loading?: boolean;
  className?: string;
}

/**
 * Componente centralizado de tarjeta de estadísticas / métricas.
 * 
 * TEMA SELECCIONADO: Pizarra Oscuro (Slate 75% con efecto Glassmorphism)
 * - Fondo translúcido con desenfoque de fondo (backdrop-blur-md)
 * - Títulos en slate-200 (+5% brillo)
 * - Subtítulos en slate-300 (+5% brillo)
 * - Cifras y números en blanco puro (text-white)
 * - Íconos unificados en blanco puro (text-white)
 * 
 * ALTERNATIVA (Negro Zinc 75% comentada para referencia futura):
 * // cardBg: 'bg-zinc-900/75 backdrop-blur-md shadow-md'
 * // cardBorder: 'border-zinc-800/75'
 * // cardHoverBorder: 'hover:border-zinc-500 hover:bg-zinc-900/85'
 * // labelColor: 'text-zinc-200'
 * // valueColor: 'text-white'
 * // sublabelColor: 'text-zinc-300'
 * // iconBg: 'bg-zinc-800/55'
 * // iconBorder: 'border-zinc-700/55'
 * // iconColor: 'text-white'
 */
export default function StatCard({
  titulo,
  valor,
  subtitulo,
  icon,
  onClick,
  loading = false,
  className = '',
}: StatCardProps) {
  const Component = onClick ? 'button' : 'div';

  return (
    <Component
      onClick={onClick}
      className={`bg-slate-900/75 backdrop-blur-md shadow-md p-5 lg:p-6 rounded-xl border border-slate-800/75 transition-all text-left w-full ${
        onClick ? 'cursor-pointer hover:border-slate-500 hover:bg-slate-900/85 hover:shadow-sm' : ''
      } ${className}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-medium text-slate-200 truncate">{titulo}</h3>
          <p className="text-2xl lg:text-3xl font-bold mt-2 text-white truncate">
            {loading ? '—' : valor}
          </p>
          {subtitulo && (
            <p className="text-xs lg:text-sm mt-2 text-slate-300 truncate">
              {loading ? '' : subtitulo}
            </p>
          )}
        </div>
        <div className="bg-slate-800/55 p-3 rounded-xl shadow-xs border border-slate-700/55 flex-shrink-0 text-white">
          {icon}
        </div>
      </div>
    </Component>
  );
}
