'use client';

import { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Venta } from '@/types/venta';
import { deshacerEntregaVenta } from '@/lib/api';

interface DeshacerEntregaModalProps {
  venta: Venta;
  onClose: () => void;
  /** Recibe la venta ya actualizada, tal como la devuelve el backend. */
  onHecho: (venta: Venta) => void;
}

/**
 * Corregir una entrega marcada por error: ENTREGADO -> PENDIENTE (solo ADMIN),
 * con confirmación. Queda registrado en la auditoría. Al volver a PENDIENTE
 * reaparece el ícono para marcarla como entregada.
 */
export default function DeshacerEntregaModal({ venta, onClose, onHecho }: DeshacerEntregaModalProps) {
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConfirmar = async () => {
    setProcesando(true);
    setError(null);
    try {
      const actualizada = await deshacerEntregaVenta(venta.id);
      onHecho(actualizada);
      onClose();
    } catch (err: any) {
      setError(err.message || 'No se pudo corregir la entrega');
    } finally {
      setProcesando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-3">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-5">
        <div className="flex items-center gap-3 mb-3">
          <div className="flex-shrink-0 w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center">
            <AlertTriangle className="text-amber-600" size={20} />
          </div>
          <div>
            <h3 className="text-base font-bold text-gray-900">Corregir a Pendiente</h3>
            <p className="text-sm text-gray-600 mt-1">
              La venta #{venta.id} volverá a Pendiente de entrega. Queda registrado en la auditoría.
            </p>
          </div>
        </div>

        {error && (
          <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{error}</div>
        )}

        <div className="flex justify-end gap-3 mt-4">
          <button
            onClick={onClose}
            disabled={procesando}
            className="px-3.5 py-1.5 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={handleConfirmar}
            disabled={procesando}
            className="px-3.5 py-1.5 bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition-colors disabled:opacity-60"
          >
            {procesando ? 'Procesando...' : 'Corregir a Pendiente'}
          </button>
        </div>
      </div>
    </div>
  );
}
