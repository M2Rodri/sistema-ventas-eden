'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';

/** Paginación con el rango real de IDs visibles, botones redondos y flechas compactas. */

interface PaginacionProps {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  itemsPerPage: number;
  onPageChange: (page: number) => void;
  itemName?: string;
  startItem?: number | string;
  endItem?: number | string;
}

function getPageNumbers(currentPage: number, totalPages: number): (number | '...')[] {
  const pages: (number | '...')[] = [];

  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) pages.push(i);
    return pages;
  }

  pages.push(1);
  if (currentPage > 3) pages.push('...');

  const start = Math.max(2, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);
  for (let i = start; i <= end; i++) pages.push(i);

  if (currentPage < totalPages - 2) pages.push('...');
  pages.push(totalPages);

  return pages;
}

export default function Paginacion({
  currentPage,
  totalPages,
  totalItems,
  itemsPerPage,
  onPageChange,
  itemName = 'productos',
  startItem,
  endItem,
}: PaginacionProps) {
  if (totalPages <= 1) return null;

  // Si se pasan startItem y endItem explicitos (ej. cuando la lista viene ordenada desc por ID),
  // se muestran exactamente esos valores. Si no, se calcula por posicion ordinal.
  const from = startItem !== undefined ? startItem : (currentPage - 1) * itemsPerPage + 1;
  const to = endItem !== undefined ? endItem : Math.min(currentPage * itemsPerPage, totalItems);
  const pages = getPageNumbers(currentPage, totalPages);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 border-t border-gray-200">
      {/* Info: Rango de IDs reales adaptado a la tabla */}
      <p className="text-sm text-gray-600 order-2 sm:order-1">
        Mostrando <span className="font-semibold text-gray-900">{from}</span> -{' '}
        <span className="font-semibold text-gray-900">{to}</span> de{' '}
        <span className="font-semibold text-gray-900">{totalItems}</span> {itemName}
      </p>

      {/* Botones — versión móvil */}
      <div className="flex items-center gap-2 sm:hidden order-1">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="inline-flex items-center justify-center w-8 h-8 rounded-full border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white"
          title="Anterior"
          aria-label="Anterior"
        >
          <ChevronLeft size={16} />
        </button>
        <span className="text-sm text-gray-600 whitespace-nowrap">
          Pág <span className="font-semibold">{currentPage}</span> de{' '}
          <span className="font-semibold">{totalPages}</span>
        </span>
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="inline-flex items-center justify-center w-8 h-8 rounded-full border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white"
          title="Siguiente"
          aria-label="Siguiente"
        >
          <ChevronRight size={16} />
        </button>
      </div>

      {/* Botones — versión escritorio (redonditos, sin texto, con flechas compactas) */}
      <nav className="hidden sm:flex items-center gap-1.5 order-2" aria-label="Paginación">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="inline-flex items-center justify-center w-8 h-8 rounded-full border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white"
          title="Anterior"
          aria-label="Anterior"
        >
          <ChevronLeft size={16} />
        </button>

        {pages.map((page, idx) =>
          page === '...' ? (
            <span key={`dots-${idx}`} className="px-1 text-sm text-gray-400">
              …
            </span>
          ) : (
            <button
              key={page}
              onClick={() => onPageChange(page)}
              className={`inline-flex items-center justify-center w-8 h-8 text-sm font-medium rounded-full transition-colors ${
                page === currentPage
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              {page}
            </button>
          ),
        )}

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="inline-flex items-center justify-center w-8 h-8 rounded-full border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white"
          title="Siguiente"
          aria-label="Siguiente"
        >
          <ChevronRight size={16} />
        </button>
      </nav>
    </div>
  );
}
