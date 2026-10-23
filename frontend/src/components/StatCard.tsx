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

/** Componente centralizado de tarjeta de estadísticas / métricas. */
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
          <p className="text-lg lg:text-xl font-bold mt-2 text-white truncate">
            {loading ? '—' : valor}
          </p>
          {subtitulo && (
            <p className="text-xs mt-2 text-slate-300 truncate">
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
